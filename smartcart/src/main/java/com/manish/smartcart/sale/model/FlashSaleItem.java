package com.manish.smartcart.sale.model;

import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Represents a Seller's specific SKU (Variant) opted-in to a PlatformSaleEvent.
 * Handled with strict concurrency controls and anti-bot measures.
 */
@Entity
@Table(name = "flash_sale_items", indexes = {
        @Index(name = "idx_fsi_approval", columnList = "approval_status"),
        @Index(name = "idx_fsi_event", columnList = "platform_sale_event_id"),
        @Index(name = "idx_fsi_variant", columnList = "product_variant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FlashSaleItem extends BaseEntity {

    // Links this specific discount to the Admin's global marketing event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_sale_event_id", nullable = false)
    private PlatformSaleEvent platformSaleEvent;

    // Links strictly to a Variant (SKU) so sellers can discount a "Large" shirt but not a "Small"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    // Security: Validates ownership so Seller A cannot modify Seller B's sale item
    @Column(nullable = false)
    private Long sellerId;

    // The temporary override discount (e.g., 20.00 = 20% off)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // Absolute maximum inventory allowed to be sold at this discounted price
    @Column(nullable = false)
    private Integer maxUnits;

    // BOT PROTECTION: Prevents scalping scripts from buying the entire maxUnits inventory.
    @Column(nullable = false)
    @Builder.Default
    private Integer maxUnitsPerUser = 1;

    // Tracks actual units consumed during checkout.
    // Protected by atomic DB updates in the Repository to prevent overselling.
    @Column(nullable = false)
    @Builder.Default
    private Integer usedUnits = 0;

    // Requires Admin QC before the discount goes live
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
}
