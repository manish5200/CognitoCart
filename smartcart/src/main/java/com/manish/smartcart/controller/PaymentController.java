package com.manish.smartcart.controller;

import com.manish.smartcart.dto.order.PaymentVerificationRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.PaymentStatus;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.service.InvoiceService;
import com.manish.smartcart.service.PaymentService;
import com.manish.smartcart.service.WebhookDlqService;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import io.swagger.v3.oas.annotations.Operation;
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

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;
    private final InvoiceService  invoiceService;
    private final WebhookDlqService  webhookDlqService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/verify")
    @Transactional
    @Operation(summary = "Verify Razorpay Payment Signature")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {

        // 1. Verify the signature cryptographically to prevent spoofing
        boolean isValid = paymentService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!isValid) {
            log.warn("Payment verification failed for Razorpay Order ID: {}", request.getRazorpayOrderId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Payment verification failed. Invalid signature."));
        }

        // 2. Find the local order with items eagerly fetched (prevents
        // LazyInitializationException)
        Order order = orderRepository.findByRazorpayOrderIdWithItems(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Order not found across this Razorpay ID: " + request.getRazorpayOrderId()));

        // 3. Prevent duplicate processing
        if (order.getOrderStatus() == OrderStatus.PAID) {
            return ResponseEntity.ok(Map.of("message", "Payment already verified successfully."));
        }

        // 4. Update order + payment status and save Razorpay IDs
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.PAID);          // ← payment lifecycle
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());
        orderRepository.save(order);

        // 5. Fire exactly one Order Confirmation Email now that money is secured
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
        orderNotificationService.sendEmailNotification(orderResponse);

        // Generate and email PDF invoice now that payment is confirmed
        byte[] invoicePdf = invoiceService.generateInvoice(orderResponse);
        orderNotificationService.sendInvoiceEmail(orderResponse, invoicePdf);

        return ResponseEntity.ok(Map.of(
                "message", "Payment verified successfully",
                "localOrderId", order.getId(),
                "razorpayPaymentId", request.getRazorpayPaymentId()));
    }

    /**
     * BACKEND-TO-BACKEND WEBHOOK
     * Razorpay calls this URL directly if the user's browser closes early.
     * Webhook URL format: https://your-domain.ngrok.io/api/v1/payments/webhook
     */
    @PostMapping("/webhook")
    @Operation(summary = "Razorpay Webhook Listener")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // 1. Verify the webhook signature using the specific Webhook Secret
            boolean isSignatureValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

            if (!isSignatureValid) {
                log.error("Invalid Webhook Signature Detected!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            // 2. Parse the payload JSON
            JSONObject jsonPayload = new JSONObject(payload);
            String eventType = jsonPayload.getString("event");

            if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
                // Extract IDs from payload
                JSONObject paymentEntity = jsonPayload.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");
                String razorpayPaymentId = paymentEntity.getString("id");

                // Find local order and promote to PAID if not already done by frontend
                orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
                    if (order.getOrderStatus() != OrderStatus.PAID) {
                        log.info("Webhook: promoting Order {} to PAID", razorpayOrderId);
                        order.setOrderStatus(OrderStatus.PAID);
                        order.setPaymentStatus(PaymentStatus.PAID);   // ← payment lifecycle
                        order.setRazorpayPaymentId(razorpayPaymentId);
                        orderRepository.save(order);

                        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
                        orderNotificationService.sendEmailNotification(orderResponse);

                        // Generate and email PDF invoice now that payment is confirmed
                        byte[] invoicePdf = invoiceService.generateInvoice(orderResponse);
                        orderNotificationService.sendInvoiceEmail(orderResponse, invoicePdf);
                    } else {
                        log.info("Webhook ignored: Order {} already PAID by frontend.", razorpayOrderId);
                    }
                });

            } else if ("payment.failed".equals(eventType)) {
                // Mark payment as FAILED so admin can query failed payments easily
                JSONObject paymentEntity = jsonPayload.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");
                String razorpayOrderId = paymentEntity.getString("order_id");

                orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
                    if (order.getPaymentStatus() != PaymentStatus.PAID) {
                        log.warn("Webhook: payment FAILED for Order {}", razorpayOrderId);
                        order.setPaymentStatus(PaymentStatus.FAILED);  // ← payment lifecycle
                        orderRepository.save(order);
                    }
                });
            }

            // Always return 200 OK to Razorpay so they don't retry the webhook
            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            log.error("Error processing Razorpay Webhook", e);

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
