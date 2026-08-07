package com.manish.smartcart.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    // ─── 3-ID SYSTEM (public-facing only) ────────────────────────────────────
    // Internal Long PKs are NEVER exposed. Frontend always works with UUIDs.
    private UUID variantPublicId;       // UUID of this variant (use for add-to-cart, stock PATCH)
    private UUID productPublicId;       // UUID of the parent product

    // ─── PRODUCT CONTEXT ──────────────────────────────────────────────────────
    // Included so clients don't need a second round-trip when listing variants
    private String productCode;         // Human-readable product code (e.g., PRD-20260808-XXXXX)
    private String productName;         // Snapshot of parent product name

    // ─── SKU IDENTITY ─────────────────────────────────────────────────────────
    private String sku;
    private BigDecimal priceModifier;   // Delta on top of the parent product's base price

    // ─── STOCK ────────────────────────────────────────────────────────────────
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableStock;     // stockQuantity - reservedQuantity (safe-to-buy count)
    private Integer lowStockThreshold;

    // ─── ATTRIBUTES ───────────────────────────────────────────────────────────
    private Map<String, String> attributes; // e.g. {"Size": "L", "Color": "Red"}

    // ─── LOGISTICS ────────────────────────────────────────────────────────────
    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    // ─── DISPLAY ──────────────────────────────────────────────────────────────
    private String variantImageUrl;     // SKU-specific swatch image
    private Integer sortOrder;
    private boolean isActive;
    private String displayLabel;        // Pre-computed "L / Red" label for frontend dropdowns
}
