package com.manish.smartcart.model.order;

import com.manish.smartcart.enums.DiscountType;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @NotBlank(message = "Coupon code cannot be blank")
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    // CONCEPT: A coupon can be a FLAT amount (₹500 off) or a PERCENTAGE (20% off).
    // The EnumType.STRING tells Hibernate to save "PERCENTAGE" in the DB, not "0" or "1"
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DiscountType discountType = DiscountType.PERCENTAGE;

    // CONCEPT: This replaces the old "discountPercentage".
    // If type is PERCENTAGE, this value is 20. If type is FLAT, this value is 500.
    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    // CONCEPT: "Cart Total must be above ₹1000 to use this code"
    // We leave this null if there is no minimum order requirement.
    private BigDecimal minOrderAmount;

    // CONCEPT: "20% off UP TO ₹500". This stops someone from buying a ₹1,00,000 TV
    // and getting ₹20,000 off!
    private BigDecimal maxDiscountAmount;

    // CONCEPT: The start date of the sale (e.g., Diwali sale starts tomorrow).
    private LocalDateTime validFrom;

    // CONCEPT: The total number of times *the entire platform* can use this coupon.
    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    // CONCEPT: How many times *a single user* can use this. Usually 1.
    private Integer maxUsesPerUser;

    @Builder.Default
    private Integer currentUses = 0;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean isActive = true;

    // CONCEPT: "WELCOME100" can only be used on the user's very first purchase.
    @Builder.Default
    private Boolean isFirstOrderOnly = false;

    /**
     * Helper to check if the coupon is currently valid overall.
     * Note: We cannot check user-specific rules here (like maxUsesPerUser)
     * because this Entity doesn't know *who* is applying the coupon. We do that in the Service.
     */
    public boolean isValid() {
        if (!isActive)
            return false;

        // Has it expired yet?
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now()))
            return false;

        // Has the sale started yet?
        if (validFrom != null && validFrom.isAfter(LocalDateTime.now()))
            return false;

        // Are there any global uses left?
        return maxUses == null || currentUses < maxUses;
    }
}
