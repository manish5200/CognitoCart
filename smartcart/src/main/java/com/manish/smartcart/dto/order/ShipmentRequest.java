package com.manish.smartcart.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * What the admin sends in the request body to attach tracking to an order.
 * @Valid is applied on the controller to enforce these constraints automatically.
 */
@Data
public class ShipmentRequest {

    @NotBlank(message = "Courier name is required (e.g., BlueDart, Delhivery)")
    private String courierName;

    @NotBlank(message = "Tracking number (AWB) is required")
    private String trackingNumber;

    // Optional — admin can provide or we auto-build it from the tracking number
    private String trackingUrl;

    @NotNull(message = "Estimated delivery date is required")
    private LocalDate estimatedDeliveryDate;

    // Optional — identifies who dispatched this package for audit purposes
    private String dispatchedBy;
}
