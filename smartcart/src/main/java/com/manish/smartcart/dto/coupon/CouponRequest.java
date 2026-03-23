package com.manish.smartcart.dto.coupon;

import com.manish.smartcart.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequest {

    @NotBlank(message = "Coupon code cannot be blank")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount value must be 0 or greater")
    private BigDecimal discountValue;

    // Optional rules
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime validFrom;

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private Integer maxUsesPerUser;
    private Boolean isFirstOrderOnly;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;

    // Advanced Promotion Fields
    private Long applicableCategoryId;
    private Long applicableProductId;
    private Integer buyXQuantity;
    private Integer getYQuantity;
    private Boolean isAutoApplied;
    private Long targetUserId;
    private BigDecimal globalBudgetLimit;

}
