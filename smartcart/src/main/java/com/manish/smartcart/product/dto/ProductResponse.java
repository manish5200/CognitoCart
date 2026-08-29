package com.manish.smartcart.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductResponse {
    // ─── 3-ID SYSTEM ──────────────────────────────────────────────────────────
    // Internal Long id is NEVER returned. Frontend always identifies products
    // by productPublicId (UUID) for API calls, and productCode for human display.
    private UUID productPublicId;           // Use this in all API calls (e.g., /products/{productPublicId}/variants)
    private String productCode;             // Human-readable ID (e.g., PRD-20260808-XXXXX)
    private String slug;                    // SEO-friendly slug for public URLs

    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    // Variant-level fields (sku, stockQuantity) intentionally omitted.
    // Use GET /api/v1/products/{productPublicId}/variants to get per-SKU inventory details.
    private Long categoryId;
    private String categoryName;
    private Set<String> tags;
    private Double averageRating;
    private Integer totalReviews;
    private List<String> imageUrls;
    private String aiSummary;               // From ProductInsights
    private LocalDateTime insightLastGenerated;
    private Integer totalSold;
}
