package com.manish.smartcart.shared.enums;

import lombok.Getter;

@Getter
public enum ShipmentStatus {

    CREATED("Shipment created and awaiting pickup", false),
    PICKED("Package picked up by courier", false),
    IN_TRANSIT("In transit to destination hub", false),
    OUT_FOR_DELIVERY("Out for delivery to customer", false),

    // Terminal States (The lifecycle ends here)
    DELIVERED("Successfully delivered", true),
    FAILED("Delivery attempt failed", true),
    RETURNED("Returned to seller", true);

    private final String displayMessage;
    private final boolean isTerminal; // True if no further updates are expected

    ShipmentStatus(String displayMessage, boolean isTerminal) {
        this.displayMessage = displayMessage;
        this.isTerminal = isTerminal;
    }

    /**
     * Optional: Prevents illegal state transitions in your service layer.
     * e.g., A shipment cannot go from DELIVERED back to IN_TRANSIT.
     */
    public boolean canTransitionTo(ShipmentStatus nextStatus) {
        if (this.isTerminal) {
            return false; // Once it's terminal, it shouldn't change.
        }
        // You can add stricter rules here, e.g., CREATED can only go to PICKED.
        return this.ordinal() < nextStatus.ordinal();
    }
}