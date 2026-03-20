package com.manish.smartcart.enums;

/**
 * Tracks whether an Admin has successfully replayed this failed webhook yet.
 */
public enum DlqStatus {
    PENDING,  // The webhook failed and is waiting for Admin review
    RESOLVED  // An Admin successfully "Replayed" the payload and it worked!
}
