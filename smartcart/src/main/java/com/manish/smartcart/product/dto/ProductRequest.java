package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.Condition;
import com.manish.smartcart.shared.enums.product.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;

    @Min(value = 0, message = "Discount price must be positive")
    private BigDecimal discountPrice;

    private String sku; // Optional: If empty, service generates one

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category Public ID is required")
    private UUID categoryPublicId;

    private Set<String> tags;

    // ─── NEW CORE DOMAIN FIELDS ──────────────────────────────
    @NotNull(message = "Country of origin is required")
    private String countryOfOrigin;

    @NotNull(message = "Condition is required")
    private Condition condition;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    // ─── NEW EMBEDDABLE & JSONB ─────────────────────────────

    private Map<String, String> attributes;

    private ProductWarrantyRequest warranty;

    private ProductSEORequest seo;

    // ─── NEW RELATIONSHIPS ───────────────────────────────────

    @Valid
    private List<ProductMediaRequest> mediaGallery;

    // All inventory and SKUs are now strictly managed inside Variants
    @Valid
    private List<ProductVariantRequest> variants;

    // Tells the backend if this is a draft. Defaults to false.
    @Builder.Default
    private Boolean isDraft = false;
}