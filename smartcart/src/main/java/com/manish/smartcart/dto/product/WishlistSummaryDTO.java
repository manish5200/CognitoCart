package com.manish.smartcart.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record WishlistSummaryDTO(
        List<ProductResponse> items,
        Integer totalItems,
        BigDecimal totalValue
){}
