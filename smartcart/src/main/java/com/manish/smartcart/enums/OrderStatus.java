package com.manish.smartcart.enums;
public enum OrderStatus {
    CREATED,              // Order placed
    PAYMENT_PENDING,      // Waiting for payment
    PAID,                 // Payment success event received
    CONFIRMED,            // Seller/system confirmed
    PACKED,              // Warehouse processing
    SHIPPED,              // Handed to courier
    DELIVERED,            // Completed
    CANCELLED,            // Cancelled before shipping
    RETURN_REQUESTED,     // Customer requested return
    RETURNED,             // Item received back
    REFUNDED              // Refund completed
}
