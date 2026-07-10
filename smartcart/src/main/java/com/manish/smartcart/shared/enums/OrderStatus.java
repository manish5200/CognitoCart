package com.manish.smartcart.shared.enums;

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
    REPLACEMENT_SHIPPED,    // Replacement unit dispatched to customer

    // ── Failure / Admin States ────────────────────────────────────────────
    // SAGA PATTERN: Cancellation committed, but Razorpay refund failed.
    // Stock is restored. Customer's cancellation is confirmed.
    // An admin must manually trigger the refund from the Razorpay dashboard.
    // An async sweep job can poll for this status and auto-retry.
    MANUAL_REFUND_REQUIRED
}
