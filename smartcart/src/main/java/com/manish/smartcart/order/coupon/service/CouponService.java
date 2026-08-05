package com.manish.smartcart.order.coupon.service;

import com.manish.smartcart.order.coupon.dto.CouponRequest;
import com.manish.smartcart.order.coupon.dto.CouponResponse;
import com.manish.smartcart.order.coupon.repository.CouponRepository;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.repository.UserCouponUsageRepository;
import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.order.coupon.model.Coupon;

import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Core Campaign and Discount Engine.
 * Architecture Note: Handles idempotent operations for distributed rollbacks
 * and strictly prevents in-memory data processing for scalable performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final UserCouponUsageRepository userCouponUsageRepository;

    // ─────────────────────────────────────────────────────────────────────────────
    // COUPON LIFECYCLE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────────
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String code = request.getCode().toUpperCase().trim();

        if (couponRepository.existsByCode(code)) {
            throw new BusinessLogicException("Coupon with code '" + code + "' already exists.");
        }
        if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("Expiry date must be in the future.");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setMaxUsesPerUser(request.getMaxUsesPerUser());
        coupon.setIsFirstOrderOnly(request.getIsFirstOrderOnly() != null ? request.getIsFirstOrderOnly() : false);
        coupon.setCurrentUses(0);
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setIsActive(true);

        // Advanced Fields
        coupon.setApplicableCategoryId(request.getApplicableCategoryId());
        coupon.setApplicableProductId(request.getApplicableProductId());
        coupon.setBuyXQuantity(request.getBuyXQuantity());
        coupon.setGetYQuantity(request.getGetYQuantity());
        coupon.setIsAutoApplied(request.getIsAutoApplied() != null ? request.getIsAutoApplied() : false);
        coupon.setTargetUserId(request.getTargetUserId());
        coupon.setGlobalBudgetLimit(request.getGlobalBudgetLimit());
        coupon.setCurrentBudgetUsed(BigDecimal.ZERO);

        Coupon saved = couponRepository.save(coupon);
        log.info("New promotional campaign created: Code [{}]", code);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void toggleActive(java.util.UUID couponPublicId) {
        Coupon coupon = couponRepository.findByPublicId(couponPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + couponPublicId));
        coupon.setIsActive(!coupon.getIsActive());
        couponRepository.save(coupon);
        log.info("Coupon ID {} activation status toggled to {}", couponPublicId, coupon.getIsActive());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TRANSACTIONAL USAGE TRACKING
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Increments simple usage limits.
     * Note: If you are tracking financial budgets, prefer recordCouponSuccess().
     */
    @Transactional
    public void incrementUsage(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        couponRepository.save(coupon);
    }

    /**
     * COMPENSATING TRANSACTION: Reverses a coupon use when an order is canceled.
     * * Architecture Note: Idempotent by design. `Math.max(0, ...)` ensures we never
     * go negative due to race conditions or duplicate webhook events from Razorpay.
     * Uses `ifPresent` — if a coupon was hard-deleted mid-flight, we silently skip
     * rather than throwing an exception that would block inventory restoration.
     */
    @Transactional
    public void decrementUsage(String code){
        couponRepository.findByCode(code.toUpperCase().trim()).ifPresent(coupon -> {
            coupon.setCurrentUses(Math.max(0, coupon.getCurrentUses() - 1));
            couponRepository.save(coupon);
            log.info("Coupon usage decremented for code: {}", code);
        });
    }

    /**
     * Financial tracking for marketing budgets.
     * Used when an order is successfully PAID to deduct from the campaign's allowed to spend.
     */
    @Transactional
    public void recordCouponSuccess(String code, BigDecimal moneySavedByUser) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);

        if (coupon.getCurrentBudgetUsed() == null) {
            coupon.setCurrentBudgetUsed(BigDecimal.ZERO);
        }

        coupon.setCurrentBudgetUsed(coupon.getCurrentBudgetUsed().add(moneySavedByUser));
        couponRepository.save(coupon);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // VALIDATION & LOOKUPS
    // ─────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
    }

    /**
     * NON-THROWING lookup.
     * Architecture Note: Used by CartService calculations. If a CartService method is
     * @Transactional and catches a thrown Exception here, Spring will still mark the
     * transaction as `rollback-only`, silently crashing the next database save.
     * Returning null avoids transaction pollution.
     */
    @Transactional(readOnly = true)
    public Coupon findActiveCouponByCode(String code) {
        return couponRepository.findByCode(code.toUpperCase().trim())
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElse(null);
    }

    /**
     * Comprehensive Validation Engine.
     * Executed strictly before applying discounts to a cart or confirming checkout.
     */
    @Transactional(readOnly = true)
    public Coupon validateCouponForCart(String couponCode, Long userId, BigDecimal grossSubTotal) {
        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + couponCode));

        // 1. Global Campaign Rules
        if (!coupon.isValidForUser(userId) || !coupon.getIsActive()) {
            throw new BusinessLogicException("Coupon is invalid, inactive, expired, or the campaign budget has been exhausted.");
        }

        // 2. Minimum Spend Requirement
        if (coupon.getMinOrderAmount() != null && grossSubTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessLogicException("Cart total must be at least ₹" + coupon.getMinOrderAmount() + " to use this coupon.");
        }

        // 3. User-Level Exhaustion Check
        if (coupon.getMaxUsesPerUser() != null) {
            userCouponUsageRepository.findByUserIdAndCouponId(userId, coupon.getId()).ifPresent(usage -> {
                if (usage.getUsage() >= coupon.getMaxUsesPerUser()) {
                    throw new BusinessLogicException("You have already reached the usage limit for this coupon.");
                }
            });
        }

        // 4. Check "First Order Only" Constraint
        if (coupon.getIsFirstOrderOnly()) {
            Set<OrderStatus> successStatuses = Set.of(
                    OrderStatus.PAID, OrderStatus.DELIVERED,
                    OrderStatus.SHIPPED, OrderStatus.CONFIRMED
            );
            // Count how many successful/paid orders the user has
            //We now delegate the count strictly to the database engine
            long pastSuccessfulOrders = orderRepository.countByUserIdAndOrderStatusIn(userId, successStatuses);
            // If they only have CANCELLED or PAYMENT_PENDING orders, pastSuccessfulOrders
            // will be 0!
            if (pastSuccessfulOrders > 0) {
                throw new BusinessLogicException("This coupon is only valid for first-time buyers.");
            }
        }
        return coupon;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────────

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(), coupon.getCode(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(), coupon.getMaxUses(),
                coupon.getCurrentUses(), coupon.getMaxUsesPerUser(),
                coupon.getIsFirstOrderOnly(), coupon.getValidFrom(),
                coupon.getExpiryDate(), coupon.getIsActive(),
                coupon.getCreatedAt(), coupon.getApplicableCategoryId(),
                coupon.getApplicableProductId(), coupon.getBuyXQuantity(),
                coupon.getGetYQuantity(), coupon.getIsAutoApplied(),
                coupon.getTargetUserId(), coupon.getGlobalBudgetLimit(),
                coupon.getCurrentBudgetUsed());
    }
}
