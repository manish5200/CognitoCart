package com.manish.smartcart.enums;
public enum PaymentStatus {
    INITIATED,     // Payment intent created
    AUTHORIZED,    // Amount blocked
    CAPTURED,      // Money debited
    FAILED,        // Payment failed
    CANCELLED,     // User cancelled
    REFUNDED       // Money returned
}
