package com.manish.smartcart.controller;

import com.manish.smartcart.dto.webhook.LogisticsWebhookRequest;
import com.manish.smartcart.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives real-time shipment status pushes from logistics carriers.

 * CONCEPT — Backend-to-Backend Webhook:
 * This controller is NOT called by the customer or admin UI.
 * The carrier's system (BlueDart, Delhivery) calls this automatically
 * when a package status changes — exactly like Razorpay webhook.

 * SECURITY NOTE (for production hardening):
 * This endpoint should be further secured with:
 * 1. IP allowlisting — accept only from carrier's known IP ranges
 * 2. HMAC signature header verification — same pattern as X-Razorpay-Signature
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Logistics Webhooks",
        description = "Backend-to-backend endpoints for carrier shipment status pushes")
public class LogisticsWebhookController {

    private final ShipmentService shipmentService;

    @PostMapping("/logistics")
    @Operation(
            summary = "Receive carrier shipment status update",
            description = "Called by logistics partners to push real-time package status changes. " +
                    "Automatically updates order status and triggers customer notification email. " +
                    "Fully idempotent — safe to call multiple times with same payload."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status update processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload — missing required fields"),
            @ApiResponse(responseCode = "404", description = "No shipment found for the given tracking number")
    })
    public ResponseEntity<?>handleLogisticsWebhook(
            @Valid @RequestBody LogisticsWebhookRequest request){
        log.info("📦 Logistics webhook received | Carrier: {} | AWB: {} | Status: {}",
                request.getCarrierName(),
                request.getTrackingNumber(),
                request.getStatus());
        shipmentService.processLogisticsUpdate(request);

        // Always respond 200 immediately — carrier systems retry on any other status.
        // Email notification runs on a background @Async thread — doesn't delay this response.
        return ResponseEntity.ok(Map.of(
                "Message","Status update received and processed.",
                "trackingNumber",request.getTrackingNumber(),
                "newStatus", request.getStatus()
        ));

    }
}
