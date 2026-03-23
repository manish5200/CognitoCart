package com.manish.smartcart.model.order;

import com.manish.smartcart.enums.DiscountType;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
import org.hibernate.annotations.SoftDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table(name = "coupons")
@SoftDelete(columnName = "is_deleted")
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
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount value must be 0 or greater")
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

    // --- ADVANCED PROMOTIONS ---
    @Column(name = "applicable_category_id")
    private Long applicableCategoryId;

    @Column(name = "applicable_product_id")
    private Long applicableProductId;

    @Column(name = "buy_x_quantity")
    private Integer buyXQuantity;

    @Column(name = "get_y_quantity")
    private Integer getYQuantity;

    @Column(name = "is_auto_applied")
    @Builder.Default
    private Boolean isAutoApplied = false;

    @Column(name = "target_user_id")
    private Long targetUserId;

    // --- GLOBAL BUDGET SAFEGUARDS ---
    @Column(name = "global_budget_limit")
    private BigDecimal globalBudgetLimit;

    @Column(name = "current_budget_used")
    @Builder.Default
    private BigDecimal currentBudgetUsed = BigDecimal.ZERO;

    // --- UPDATE YOUR isValid() METHOD TO CHECK BUDGET AND TARGET USER ---
    public boolean isValidForUser(Long requestingUserId) {
        if (!isValid()) return false;

        // 1. Check if the company ran out of Marketing Budget for this Coupon!
        if (globalBudgetLimit != null && currentBudgetUsed.compareTo(globalBudgetLimit) >= 0) {
            return false; // Campaign budget exhausted!
        }

        // 2. If it was auto-generated for an Abandoned Cart Email, block other users from stealing it
        if (targetUserId != null && !targetUserId.equals(requestingUserId)) {
            return false;
        }

        return true;
    }

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
