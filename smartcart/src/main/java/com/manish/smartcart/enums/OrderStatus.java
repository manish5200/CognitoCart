package com.manish.smartcart.enums;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CONFIRMED,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,

    // ── Post-delivery customer requests ──────────────────────────────────
    RETURN_REQUESTED,       // Customer wants refund — awaiting admin approval
    REPLACEMENT_REQUESTED,  // Customer wants same item re-sent — awaiting admin check
    EXCHANGE_REQUESTED,     // Customer wants different variant — awaiting admin handling

    // ── Resolution states ─────────────────────────────────────────────────
    RETURNED,               // Item physically received back at warehouse
    REFUNDED,               // Money returned to customer via Razorpay
    REPLACEMENT_SHIPPED     // Replacement unit dispatched to customer
}
