package com.manish.smartcart.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    // Optional: Auto-generated if left blank
    private String sku;

    // e.g., Base product is ₹1000. If this is +₹200, effective price is ₹1200.
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    // Triggers notification when stock dips below this
    @Builder.Default
    private Integer lowStockThreshold = 5;

    // Dynamic key-value pairs (e.g., Size: XL, Color: Navy)
    private Map<String, String> attributes;

    // Physical characteristics (Important for future Shiprocket integration)
    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;


    @Builder.Default
    private Integer sortOrder = 0;
}
