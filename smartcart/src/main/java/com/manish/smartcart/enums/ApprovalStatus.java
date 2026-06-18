package com.manish.smartcart.enums;

/**
 * Tracks the Admin Quality Control (QC) process for Seller-submitted Flash Sale Items.
 * Prevents sellers from offering fake discounts during major events.
 */
public enum ApprovalStatus {
    PENDING,   // Seller submitted the item; waiting for Admin review.
    APPROVED,  // Admin verified the discount is legitimate. Ready for sale.
    REJECTED   // Admin denied the request (e.g., base price was manipulated).
}
