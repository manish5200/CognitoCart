package com.manish.smartcart.order.dto;

import java.math.BigDecimal;

public record PromotionResult(
        BigDecimal discountAmount,
        boolean isFreeShippingApplies
) {}
