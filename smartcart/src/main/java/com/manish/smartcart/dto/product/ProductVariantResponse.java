package com.manish.smartcart.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private Long productId; // Reference back to parent

    private String sku;
    private BigDecimal priceModifier;

    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableStock; // Dynamic calculation for frontend

    private Integer lowStockThreshold;
    private Map<String, String> attributes;

    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    private String variantImageUrl; // The SKU-specific swatch image
    private Integer sortOrder;
    private boolean isActive;

    // Helper property so frontend doesn't need to manually combine attributes
    private String displayLabel;
}
