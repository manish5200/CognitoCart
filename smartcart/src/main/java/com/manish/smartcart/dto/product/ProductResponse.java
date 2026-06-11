package com.manish.smartcart.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductResponse {
    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    // Variant-level fields (sku, stockQuantity) intentionally omitted.
    // Use GET /api/v1/products/{id}/variants to get per-SKU inventory details.
    private String categoryName;
    private Set<String> tags;
    private Double averageRating;
    private Integer totalReviews;
    private List<String> imageUrls;
    private String aiSummary;           // from ProductInsights
    private LocalDateTime insightLastGenerated; // when was this AI summary computed
}
