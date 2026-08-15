package com.manish.smartcart.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * Admin sends this when attaching tracking to a shipped order.
 * <p>
 * trackingNumber is now OPTIONAL — Shiprocket generates the AWB automatically.
 * Admin can still override it for non-Shiprocket couriers (walk-in drops etc.)
 * <p>
 * Weight + dimensions allow Shiprocket to accurately match the best courier
 * for this specific package. Defaults are suitable for small items.
 */
@Data
public class ShipmentRequest {

    @NotBlank(message = "Courier name is required (e.g., BlueDart, Delhivery)")
    private String courierName;

    // Optional — Shiprocket auto-generates AWB. Admin can override for non-Shiprocket couriers.
    private String trackingNumber;

    // Optional — auto-built from AWB if not provided
    private String trackingUrl;

    @NotNull(message = "Estimated delivery date is required")
    private LocalDate estimatedDeliveryDate;

    // Optional — who packed this (audit trail)
    private String dispatchedBy;

    // Parcel dimensions — needed for accurate Shiprocket courier assignment.
    // Default: 10×10×10 cm, 0.5 kg (suitable for small packages).
    // Admin should override for large/heavy items.
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    private Double weightInKg = 0.5;


    // Parcel dimensions in cm. Used for volumetric weight calculation.
    // Default: 10×10×10 cm (override for large/bulky items)
    private Integer lengthCm = 10;
    private Integer breadthCm = 10;
    private Integer heightCm = 10;
}
