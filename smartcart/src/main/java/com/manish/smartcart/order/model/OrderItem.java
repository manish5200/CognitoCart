package com.manish.smartcart.order.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.shared.enums.PolicyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Represents an immutable line item on a finalized order.
 * * ARCHITECTURE RULE: Dual-Layer Design
 * Balances operational needs with financial and legal compliance:
 * * - Layer 1 (Live Reference): Points to the active ProductVariant for workflows like
 * stock deduction. This is purposefully nullable to ensure order history survives
 * hard-deletions in the catalog.
 * * - Layer 2 (Immutable Snapshots): Prices, SKUs, and names are captured securely
 * at checkout. Guarantees that invoices and ledgers remain historically accurate
 * even if product details change globally.
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Entity
@Table(name = "order_items", indexes = {
        // Optimizes the mandatory fetch of all items when loading an Order details page.
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        // Optimizes reverse-lookups for seller analytics (e.g., "Which orders contain my SKU?").
        @Index(name = "idx_order_items_variant_id", columnList = "variant_id")
})
@SequenceGenerator(name = "entity_seq", sequenceName = "order_item_seq", allocationSize = 50)
public class OrderItem extends BaseEntity {

    /**
     * The parent order this item belongs to.
     **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    // Foreign key to the physical SKU.
    // Nullable fallback ensures order history remains intact during data corruption/deletions.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    private ProductVariant variant;

    @Column(nullable = false)
    @Min(value = 1, message = "Order quantity must be at least 1")
    private Integer quantity;

    // ─── FINANCIAL SNAPSHOTS (Immutable) ──────────────────────────────────────

    // The exact unit price locked in at checkout (Base Price + Modifiers - Discounts).
    // Safely consumed by Invoice and Refund services without risking live price drift.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    // Pre-computed extension (priceAtPurchase × quantity).
    // Persisted to eliminate redundant BigDecimal math during intensive PDF generation loops.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    // ─── CATALOG SNAPSHOTS (Immutable) ────────────────────────────────────────

    // Preserved master product identity (e.g., "Nike Air Max 90").
    @Column(nullable = false, length = 255)
    private String productNameSnapshot;

    // Preserved customer selection (e.g., "Navy Blue / UK 9").
    @Column(length = 100)
    private String variantLabelSnapshot;

    // Preserved warehouse routing code. Critical for dispute resolution and matching returns.
    @Column(length = 100)
    private String skuSnapshot;

    // Preserved visual identity at checkout. Mitigates "bait-and-switch" vendor modifications.
    @Column(length = 500)
    private String imageUrlSnapshot;

    // ─── RETURN POLICY SNAPSHOTS (Immutable) ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type_snapshot")
    private PolicyType policyTypeSnapshot;

    @Column(name = "return_window_days_snapshot")
    private Integer returnWindowDaysSnapshot;
}