package com.manish.smartcart.service;

import com.manish.smartcart.dto.coupon.CouponRequest;
import com.manish.smartcart.dto.coupon.CouponResponse;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Coupon;
import com.manish.smartcart.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;// Needed to check past orders
    private final UserCouponUsageRepository userCouponUsageRepository; // Needed to check per-user limits

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String code = request.getCode().toUpperCase().trim();

        if (couponRepository.existsByCode(code)) {
            throw new RuntimeException("Coupon with code '" + code + "' already exists.");
        }
        if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expiry date must be in the future.");
        }

        // Build entity with proper defaults
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setMaxUsesPerUser(request.getMaxUsesPerUser());
        // Default to false if user didn't send it in JSON
        coupon.setIsFirstOrderOnly(request.getIsFirstOrderOnly() != null ? request.getIsFirstOrderOnly() : false);
        coupon.setCurrentUses(0);
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setIsActive(true);

        Coupon saved = couponRepository.save(coupon);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void toggleActive(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon missing"));
        coupon.setIsActive(!coupon.getIsActive());
        couponRepository.save(coupon);
    }

    @Transactional
    public void incrementUsage(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Coupon missing"));
        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);
    }

    /**
     * Simple lookup by code - no validation. Used when validation already happened
     * at cart stage.
     */
    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));
    }

    /**
     * NON-THROWING lookup — returns null if coupon doesn't exist or is inactive.
     * Used by CartService.updateCartTotal() so it NEVER throws inside an active
     * transaction (which would silently mark the TX rollback-only and cause
     * UnexpectedRollbackException on the next save).
     */
    @Transactional(readOnly = true)
    public Coupon findActiveCouponByCode(String code) {
        return couponRepository.findByCode(code.toUpperCase().trim())
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElse(null);
    }

    // --- Mapper ---
    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getMaxUsesPerUser(),
                coupon.getIsFirstOrderOnly(),
                coupon.getValidFrom(),
                coupon.getExpiryDate(),
                coupon.getIsActive(),
                coupon.getCreatedAt());
    }

    /**
     * Advanced Validation: Checks global rules PLUS User-Specific and Cart-Specific
     * rules.
     */
    @Transactional(readOnly = true)
    public Coupon validateCouponForCart(String couponCode, Long userId, BigDecimal grossSubTotal) {
        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponCode));

        // 1. Check Global Rules (Is it active, expired?)
        if (!coupon.isValid() || !coupon.getIsActive()) {
            throw new RuntimeException("Coupon is invalid, inactive, or expired.");
        }

        // 2. Check Minimum Order Amount
        if (coupon.getMinOrderAmount() != null && grossSubTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException(
                    "Cart total must be at least ₹" + coupon.getMinOrderAmount() + " to use this coupon.");
        }

        // 3. Check Per-User Usage Limit
        if (coupon.getMaxUsesPerUser() != null) {
            userCouponUsageRepository.findByUserIdAndCouponId(userId, coupon.getId()).ifPresent(usage -> {
                if (usage.getUsage() >= coupon.getMaxUsesPerUser()) {
                    throw new RuntimeException("You have already reached the usage limit for this coupon.");
                }
            });
        }

        // 4. Check "First Order Only" Constraint
        if (coupon.getIsFirstOrderOnly()) {
            // Count how many successful/paid orders the user has
            long pastSuccessfulOrders = orderRepository.findByUserId(userId).stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.PAID
                            || order.getOrderStatus() == OrderStatus.DELIVERED
                            || order.getOrderStatus() == OrderStatus.SHIPPED
                            || order.getOrderStatus() == OrderStatus.CONFIRMED)
                    .count();
            // If they only have CANCELLED or PAYMENT_PENDING orders, pastSuccessfulOrders
            // will be 0!
            if (pastSuccessfulOrders > 0) {
                throw new RuntimeException("This coupon is only valid for first-time buyers.");
            }
        }
        return coupon;
    }

}
