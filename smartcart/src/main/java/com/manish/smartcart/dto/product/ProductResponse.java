package com.manish.smartcart.dto.product;

import lombok.*;

import java.math.BigDecimal;
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
    private String sku;
    private Integer stockQuantity;
    private String categoryName;
    private Set<String> tags;
    private Double averageRating;
    private Integer totalReviews;
    private List<String> imageUrls;

}
