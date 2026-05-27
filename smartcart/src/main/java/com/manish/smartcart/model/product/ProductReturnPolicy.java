package com.manish.smartcart.model.product;

import com.manish.smartcart.enums.PolicyType;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Defines the return/exchange/replacement policy for a product or an entire category.
 * RESOLUTION CHAIN (used in ReturnPolicyService):
 *   1. Does this specific product have its own policy?     → use it
 *   2. Does the product's category have a policy?          → use it
 *   3. Neither? → fall back to a safe hardcoded default    → NON_RETURNABLE, 0 days
 * The CHECK constraint in V24 guarantees exactly one of product_id/category_id is set.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Must match BaseEntity's @SuperBuilder
@Entity
@Table(name = "product_return_policy")
public class ProductReturnPolicy extends BaseEntity {

    /**
     * If set, this policy applies to this specific product only.
     * Mutually exclusive with category (enforced by DB constraint chk_policy_target).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * If set, this policy applies to ALL products in this category.
     * Mutually exclusive with product (enforced by DB constraint chk_policy_target).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /** What type of post-purchase action is permitted. Stored as a string in the DB. */
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type",  nullable = false, length = 30)
    private PolicyType policyType;

    /** How many days after delivery the customer has to initiate a return/exchange. */
    @Column(name = "return_window_days", nullable = false)
    private int returnWindowDays;

    /** Can the customer get a refund by returning the item? */
    @Column(name = "return_allowed", nullable = false)
    private boolean returnAllowed;

    /** Can the customer exchange for another variant (size/color)? */
    @Column(name = "exchange_allowed", nullable = false)
    private boolean exchangeAllowed;

    /** Will we send a replacement unit for defective items? */
    @Column(name = "replacement_allowed", nullable = false)
    private boolean replacementAllowed;

    /** Will we arrange a pickup, or does the customer need to ship it back themselves? */
    @Column(name = "pickup_available", nullable = false)
    private boolean pickupAvailable;
}
