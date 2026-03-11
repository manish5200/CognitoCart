package com.manish.smartcart.dto.coupon;

import com.manish.smartcart.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer maxUses;
    private Integer currentUses;
    private Integer maxUsesPerUser;
    private Boolean isFirstOrderOnly;
    private LocalDateTime validFrom;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
