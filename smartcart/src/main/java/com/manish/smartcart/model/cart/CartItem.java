package com.manish.smartcart.model.cart;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.product.ProductVariant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Represents a single line item within a shopping cart.
 * Domain Rule: Cart items map directly to specific ProductVariants (SKUs), not Master Products.
 * This ensures precise inventory allocation and accurate fulfillment at checkout.
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Entity
@Table(name = "cart_items",
        indexes = {
                @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
                @Index(name = "idx_cart_items_variant_id", columnList = "variant_id")
        },
        // Enforces that a cart cannot have duplicate rows for the exact same variant.
        // Subsequent additions of the same item must update the quantity of the existing row.
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cart_variant", columnNames = {"cart_id", "variant_id"})
        }
)
public class CartItem extends BaseEntity {

    // ─── RELATIONSHIPS ────────────────────────────────────────────────────────

    // Lazy-loaded reference to the specific physical SKU (e.g., Red, Size L).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    // The parent cart that owns this line item.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference  // Prevents Cart → CartItem → Cart infinite JSON loop
    private Cart cart;

    // ─── ITEM DETAILS ─────────────────────────────────────────────────────────

    // Number of units requested. Prevent zero or negative values at the DB level.
    @Column(nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Point-in-time snapshot of the variant's price when added to the cart.
    // Protects the checkout flow from unexpected mid-session price fluctuations.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdding;

    // ─── COMPUTED HELPERS ─────────────────────────────────────────────────────

    /**
     * Calculates the total subtotal for this specific line item.
     * @return priceAtAdding * quantity
     */
    public BigDecimal getSubTotal() {
        if (priceAtAdding == null || quantity == null) return BigDecimal.ZERO;
        return priceAtAdding.multiply(new BigDecimal(quantity));
    }
}