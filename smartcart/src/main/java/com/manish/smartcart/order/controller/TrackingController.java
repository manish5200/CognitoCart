package com.manish.smartcart.order.controller;

import com.manish.smartcart.order.model.Shipment;
import com.manish.smartcart.order.repository.ShipmentRepository;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/track")
@Tag(name = "Public Tracking", description = "No-auth tracking endpoints for customers")
public class TrackingController {

    private final ShipmentRepository shipmentRepository;

    /*
     * PUBLIC ENDPOINT: No JWT Required.
     * Uses the Human ID (Tracking Code) to fetch shipping status.
     * Returns a stripped-down, privacy-safe JSON response.
     */
    @Operation(summary = "Track shipment", description = "Public tracking lookup using Human Tracking Code (e.g. SHP-...)")
    @GetMapping("/{trackingCode}")
    public ResponseEntity<Map<String, Object>> trackShipment(@PathVariable String trackingCode){

        Shipment shipment = shipmentRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException("No shipment found for tracking code: " + trackingCode));

        // In a real startup, you'd use a dedicated TrackingResponse DTO here.
        // We use a Map here to strictly control the exact fields exposed to the public internet.
        // Notice we DO NOT expose the internal order ID, user details, or full street address!
        Map<String, Object> safeResponse = new HashMap<>();
        safeResponse.put("trackingCode", shipment.getTrackingCode());
        safeResponse.put("carrier", shipment.getCourierName()); // Corrected!
        safeResponse.put("carrierTrackingNumber", shipment.getTrackingNumber());
        safeResponse.put("status", shipment.getOrder().getOrderStatus());
        safeResponse.put("estimatedDelivery", shipment.getEstimatedDeliveryDate());
        safeResponse.put("destinationCity", shipment.getOrder().getShippingCity());
        safeResponse.put("destinationState", shipment.getOrder().getShippingState());

        return ResponseEntity.ok(safeResponse);
    }
}
