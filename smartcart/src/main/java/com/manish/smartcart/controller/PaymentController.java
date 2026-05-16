package com.manish.smartcart.controller;

import com.manish.smartcart.dto.order.PaymentVerificationRequest;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.service.WebhookDlqService;
import com.manish.smartcart.service.WebhookProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.razorpay.Utils;
import org.json.JSONObject;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Verification", description = "Endpoints for Razorpay Webhooks and Frontend Callbacks")
public class PaymentController {

    private final WebhookDlqService  webhookDlqService;
    private final WebhookProcessingService webhookProcessingService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    // ─── FRONTEND CALLBACK ────────────────────────────────────────────────────
    @PostMapping("/verify")
    @Transactional
    @Operation(summary = "Verify Razorpay Payment Signature (Frontend Callback)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid signature"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        // Controller responsibility: HTTP only — delegate ALL logic to service
        Order order = webhookProcessingService.verifyAndConfirmPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        return ResponseEntity.ok(Map.of(
                "message", "Payment verified successfully",
                "localOrderId", order.getId(),
                "razorpayPaymentId", request.getRazorpayPaymentId()));
    }

    // ─── RAZORPAY SERVER WEBHOOK ──────────────────────────────────────────────
    /**
     * BACKEND-TO-BACKEND WEBHOOK
     * Razorpay calls this URL directly if the user's browser closes early.
     * Webhook URL format: <a href="https://your-domain.ngrok.io/api/v1/payments/webhook">...</a>
     */
    @PostMapping("/webhook")
    @Operation(summary = "Razorpay Webhook Listener")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook received and processed"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature"),
        @ApiResponse(responseCode = "500", description = "Processing failed, saved to DLQ")
    })
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {

            // 1. Verify the webhook signature using the specific Webhook Secret - Signature verification is infrastructure
            boolean isSignatureValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

            if (!isSignatureValid) {
                log.error("Invalid Razorpay Webhook Signature Detected!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            // 2. All business logic delegated to service
            webhookProcessingService.processRazorpayWebhook(payload);
            // Always return 200 OK to Razorpay so they don't retry the webhook
            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            log.error("Error processing Razorpay Webhook", e);

            // 3. DLQ fallback — preserve for retry/audit
            // 🚨 THE DLQ INTERCEPT
            // Extract event type if possible from the payload to make searching easier
            String eventType = "UNKNOWN";
            try{
                eventType = new JSONObject(payload).optString("event", "UNKNOWN");

            }catch (Exception ignored) {}
            webhookDlqService.saveFailedWebhook(payload, signature, eventType, e.getMessage());

            // By returning 500, we STILL tell Razorpay to retry in 20 mins.
            // If Razorpay succeeds on the 2nd try, great! If it fails all 3 times,
            // we safely have it in our DB forever.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed. Saved to DLQ.");
        }
    }
}
