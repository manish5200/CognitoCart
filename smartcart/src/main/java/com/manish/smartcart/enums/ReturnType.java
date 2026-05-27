package com.manish.smartcart.enums;

/**
 * Represents what the CUSTOMER is requesting post-delivery.
 * Distinct from PolicyType (which represents what the SELLER allows).

 * PolicyType = the restaurant menu (seller's rules)
 * ReturnType = the customer's order (what they want)

 * Validation: ReturnType is checked against PolicySnapshot booleans
 *   RETURN      → requires returnAllowed = true
 *   REPLACEMENT → requires replacementAllowed = true + product in stock
 *   EXCHANGE    → requires exchangeAllowed = true
 */
public enum ReturnType {
    RETURN,       // Customer wants money back (triggers Razorpay refund)
    REPLACEMENT,  // Customer wants the same item re-sent (no refund, re-ships)
    EXCHANGE      // Customer wants a different variant: size/color (needs manual admin handling)
}
