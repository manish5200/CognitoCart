package com.manish.smartcart.wishlist.dto;

import com.manish.smartcart.product.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

public record WishlistSummaryDTO(
        List<ProductResponse> items,
        Integer totalItems,
        BigDecimal totalValue
){}
