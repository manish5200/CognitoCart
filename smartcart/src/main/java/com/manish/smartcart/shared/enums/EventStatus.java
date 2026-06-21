package com.manish.smartcart.shared.enums;


/**
 * Tracks the global lifecycle of a Platform-wide Sale Event (e.g., "Big Billion Days").
 * Driven by the automated ShedLock background job.
 */
public enum EventStatus {
    SCHEDULED, // The event is planned for the future; no discounts apply yet.
    ACTIVE,    // The event is currently live; CartService will apply discounts.
    ENDED      // The event time has passed; discounts are instantly revoked.
}
