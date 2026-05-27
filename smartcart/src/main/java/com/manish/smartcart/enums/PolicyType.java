package com.manish.smartcart.enums;

/**
 * Defines what kind of post-purchase action is allowed for a product/category.
 *
 * RETURN_AND_EXCHANGE  → Customer can return for refund OR exchange for another item
 * RETURN_ONLY          → Return for refund only; no exchanges
 * EXCHANGE_ONLY        → Can swap for a variant/size; no cash refund
 * REPLACEMENT_ONLY     → Defective item gets replaced; no refund, no exchange
 * NON_RETURNABLE       → Final sale — no post-purchase recourse (e.g., innerwear, digital goods)
 */
public enum PolicyType {
    RETURN_AND_EXCHANGE,
    RETURN_ONLY,
    EXCHANGE_ONLY,
    REPLACEMENT_ONLY,
    NON_RETURNABLE
}
