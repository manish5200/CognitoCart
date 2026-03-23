package com.manish.smartcart.dto.order;

import java.math.BigDecimal;

public record PromotionResult(
        BigDecimal discountAmount,
        boolean isFreeShippingApplies
) {}
