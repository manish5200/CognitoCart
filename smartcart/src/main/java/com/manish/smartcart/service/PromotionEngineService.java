package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.PromotionResult;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.order.Coupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Enterprise Promotions Engine Service
 * Responsible for calculating exact discount amounts for complex rules like BOGO,
 * Category-specific discounts, and Free Shipping.
 */

@Slf4j
@Service
public class PromotionEngineService {

    /**
     * Evaluates the math for a specific cart against a specific coupon.
     *
     * @param cart The user's current Cart containing items
     * @param coupon The coupon being applied (validated earlier)
     * @param grossSubTotal The sum total of all items in the cart
     * @return PromotionResult DTO containing the exact dollar discount amount and shipping flags
     */

    public PromotionResult evaluate(Cart cart, Coupon coupon, BigDecimal grossSubTotal){
        log.info("Evaluating Promotion Engine for Coupon Code: [{}]", coupon.getCode());

        // 1. Minimum Order Amount Validation
        if(coupon.getMinOrderAmount() != null &&
                grossSubTotal.compareTo(coupon.getMinOrderAmount()) < 0){
            log.warn("Coupon [{}] rejected. Cart total (₹{}) is below required minimum (₹{}).",
                    coupon.getCode(), grossSubTotal, coupon.getMinOrderAmount());

            return new PromotionResult(BigDecimal.ZERO, false);
        }
        // 2. Delegate to the correct Strategy using Modern Java 14+ Pattern Matching
        PromotionResult result = switch(coupon.getDiscountType()){
            case PERCENTAGE -> new PromotionResult(calculatePercentage(grossSubTotal, coupon),false);
            case BOGO -> new PromotionResult(calculateBogo(cart,coupon), false);
            case FLAT -> new PromotionResult(calculateFlat(grossSubTotal,coupon),false);
            case FREE_SHIPPING -> new PromotionResult(BigDecimal.ZERO, true);
        };

        log.info("Promotion Evaluation Complete. Discount applied: ₹{} | Free Shipping: {}",
                result.discountAmount(), result.isFreeShippingApplies());

        return result;
    }

    //-------------CALCULATE PERCENTAGE HELPER METHOD---------------------
    private BigDecimal calculatePercentage(BigDecimal grossSubTotal, Coupon coupon) {
        log.debug("Calculating PERCENTAGE discount ({}%) on subtotal: ₹{}",
                coupon.getDiscountValue(), grossSubTotal);

        BigDecimal discount = grossSubTotal
                .multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);

        // Cap rule: Prevent insane discounts (e.g., getting ₹50k off a TV with a 50% generic coupon)
        if(coupon.getMaxDiscountAmount() != null &&
                discount.compareTo(coupon.getMaxDiscountAmount()) > 0){
            log.debug("Percentage discount (₹{}) exceeds max cap. Capping to: ₹{}",
                    discount, coupon.getMaxDiscountAmount());

            return coupon.getMaxDiscountAmount();
        }
        return discount;
    }

    //-------------CALCULATE FLAT HELPER METHOD---------------------
    private BigDecimal calculateFlat(BigDecimal grossSubTotal, Coupon coupon) {
        log.debug("Calculating FLAT discount of ₹{} on subtotal: ₹{}",
                coupon.getDiscountValue(), grossSubTotal);
        /*Safety: If Flat discount is ₹500, but cart is only ₹100,
        we strictly cap it at ₹100 to prevent negative carts. */
        return coupon.getDiscountValue().min(grossSubTotal);
    }

    //-------------CALCULATE BOGO HELPER METHOD---------------------
    private BigDecimal calculateBogo(Cart cart, Coupon coupon) {
        log.debug("Calculating BOGO discount. Required: Buy {} Get {} of Product ID: {}",
                coupon.getBuyXQuantity(), coupon.getGetYQuantity(), coupon.getApplicableProductId());

        // Validation against bad DB setups
        if( coupon.getApplicableProductId() == null ||
                coupon.getBuyXQuantity() == null ||
                coupon.getGetYQuantity() == null){
            log.error("Fatal: BOGO Coupon [{}] has incomplete configuration in database!",
                    coupon.getCode());
            return BigDecimal.ZERO;
        }
        // 1. Scan cart to see if the required BOGO product is actually present
        CartItem targetItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(coupon.getApplicableProductId()))
                .findFirst()
                .orElse(null);

        // 2. Reject if the product is completely missing or quantity is too low
        if(targetItem == null || targetItem.getQuantity() < coupon.getBuyXQuantity()){
            log.debug("BOGO rejected. User lacked sufficient quantity of target product.");
            return BigDecimal.ZERO;
        }

        // 3. The Math: e.g., Buy 2 get 1 Free. If they buy 5 => (5 / 2) = 2. Free items = 2 * 1 = 2.
        int timesOfferApplies = targetItem.getQuantity() / coupon.getBuyXQuantity();
        int freeQuantity = timesOfferApplies * coupon.getGetYQuantity();

        // Safety Net: Never refund more shirts than they logically have
        freeQuantity = Math.min(freeQuantity, targetItem.getQuantity());

        BigDecimal totalBogoDiscount = targetItem.getPriceAtAdding()
                .multiply(BigDecimal.valueOf(freeQuantity));

        log.debug("BOGO Applied. User gets {} free items. Total BOGO deduction: ₹{}",
                freeQuantity, totalBogoDiscount);

        return totalBogoDiscount;
    }

}
