package com.manish.smartcart.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * CONCEPT: We don't want to expose the raw Shipment entity to the frontend.
 * This DTO will be embedded inside OrderResponse so the frontend can build
 * a nice visual tracking widget (like "Arriving by Friday").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingDTO {
    private String courierName;
    private String trackingNumber;
    private String trackingUrl;
    private LocalDate estimatedDeliveryDate;
}
