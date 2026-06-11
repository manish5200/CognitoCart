package com.manish.smartcart.model.product;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a physical, purchasable SKU.
 * Domain Rule: Single uniform code path for checkout and inventory.
 * Products without options automatically generate one default variant {"Type": "Standard"}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variant_sku", columnList = "sku", unique = true),
        @Index(name = "idx_variant_product_id", columnList = "product_id"),
        @Index(name = "idx_variant_active", columnList = "is_active")
})
public class ProductVariant extends BaseEntity {

    // ─── IDENTITY ─────────────────────────────────────────────────────────────

    // Lazy-loaded to prevent N+1 queries during isolated inventory checks.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    // Internal seller-defined stock keeping unit.
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    // Global retail barcode (EAN/UPC/ISBN) for POS and 3PL integrations.
    @Column(unique = true, length = 50)
    private String barcode;

    // ─── UNIVERSAL ATTRIBUTES (JSONB) ─────────────────────────────────────────

    // Flexible attribute mapping (e.g., {"Size": "L", "Color": "Red"}).
    // LinkedHashMap preserves insertion order within the request lifecycle for consistent UI rendering.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    // ─── STOCK MANAGEMENT ─────────────────────────────────────────────────────

    // Total physical inventory.
    @Column(nullable = false)
    @Min(0)
    private Integer stockQuantity;

    // Soft-allocated inventory held by active carts to prevent concurrent overselling.
    // Released upon Redis TTL expiration or successful checkout.
    @Column(nullable = false)
    @Builder.Default
    @Min(0)
    private Integer reservedQuantity = 0;

    // Inventory floor triggering automated restocking alerts.
    @Column(nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 5;

    // Note: Optimistic locking (@Version) is inherited from BaseEntity.

    // ─── PRICING ──────────────────────────────────────────────────────────────

    // Delta applied to the master Product's base price.
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    // Variant-specific sale override. Takes precedence over the master product discount.
    @Column(precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    // Cost of Goods Sold. Strictly for backend P&L calculation; never exposed to clients.
    @JsonIgnore
    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;

    // ─── LOGISTICS ────────────────────────────────────────────────────────────

    // Physical weight in kg. Nullable for digital goods.
    @Column(precision = 6, scale = 3)
    private BigDecimal weight;

    // Dimensions in cm, required for 3PL volumetric freight calculations.
    @Column(precision = 6, scale = 2)
    private BigDecimal lengthCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal widthCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal heightCm;

    // ─── DISPLAY ──────────────────────────────────────────────────────────────

    // SKU-specific swatch image. Overrides the master gallery thumbnail when selected.
    private String variantImageUrl;

    // UI rendering sequence.
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    // Soft-delete toggle. Hides the variant while preserving historical order integrity.
    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ─── COMPUTED HELPERS ─────────────────────────────────────────────────────

    /**
     * Calculates safely purchasable inventory.
     * @return Gross stock minus active cart reservations.
     */
    public int getAvailableStock() {
        return Math.max(0, this.stockQuantity - this.reservedQuantity);
    }

    /**
     * Formats attributes for frontend labels (e.g., "L / Red").
     */
    public String getDisplayLabel() {
        if (attributes == null || attributes.isEmpty()) return "Default";
        return String.join(" / ", attributes.values());
    }

    /**
     * Evaluates if available inventory has breached the restocking threshold.
     */
    public boolean isLowStock() {
        return this.getAvailableStock() <= this.lowStockThreshold;
    }

    /**
     * Calculates volumetric weight (L × W × H / 5000) for freight APIs.
     * @return Volumetric weight in kg, or null if dimensions are incomplete.
     */
    public BigDecimal getVolumetricWeight() {
        if (lengthCm == null || widthCm == null || heightCm == null) return null;
        return lengthCm.multiply(widthCm).multiply(heightCm)
                .divide(new BigDecimal("5000"), 3, RoundingMode.HALF_UP);
    }
}