package com.manish.smartcart.enums;

/**
 * Tracks the payment lifecycle independently of the order fulfillment status.
 *
 * Mapping:
 *  PENDING   → Razorpay order created, payment not yet received
 *  PAID      → Signature verified (or webhook confirmed) — money secured
 *  FAILED    → Webhook reported payment.failed event
 *  REFUNDED  → Refund issued (Phase 2 — Razorpay refund API)
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
