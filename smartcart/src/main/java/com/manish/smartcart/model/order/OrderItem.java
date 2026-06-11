package com.manish.smartcart.model.order;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.product.ProductVariant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Represents an immutable line item on a finalized order.
 * * ARCHITECTURE RULE: Employs a Dual-Layer design to balance operational needs with legal compliance.
 * * LAYER 1 — Live Reference (variant_id):
 * Points to the active ProductVariant for operational workflows (stock deduction, return processing).
 * Nullable to ensure the historical order record survives even if a variant is hard-deleted.
 * * LAYER 2 — Immutable Snapshots:
 * String and BigDecimal values captured securely at checkout.
 * Guarantees that Order Mappers and Invoice Services always generate accurate historical
 * documents, even if the seller completely alters the product's name, price, or SKU tomorrow.
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
public class OrderItem extends BaseEntity {

    // ─── LIVE REFERENCES ──────────────────────────────────────────────────────

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
}