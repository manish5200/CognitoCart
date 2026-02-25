package com.manish.smartcart.enums;

/**
 * KYC lifecycle for seller accounts.
 *
 * Flow:
 * PENDING → IN_REVIEW → VERIFIED
 * → REJECTED (can re-submit)
 * VERIFIED → SUSPENDED (admin action)
 */
public enum KycStatus {

    /** Just registered — no documents submitted yet. Default state. */
    PENDING,

    /** Seller has submitted documents; under admin review. */
    IN_REVIEW,

    /** KYC approved — full selling access granted. */
    VERIFIED,

    /** Documents rejected — seller can re-submit. */
    REJECTED,

    /** Account suspended by admin (policy violation, fraud etc.). */
    SUSPENDED
}
