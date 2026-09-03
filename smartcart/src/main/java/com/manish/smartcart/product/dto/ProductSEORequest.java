package com.manish.smartcart.product.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSEORequest {
    private String seoTitle;
    private String metaDescription;
}
