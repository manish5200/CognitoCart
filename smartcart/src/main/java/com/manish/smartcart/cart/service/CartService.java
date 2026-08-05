package com.manish.smartcart.cart.service;

import com.manish.smartcart.cart.repository.CartItemRepository;
import com.manish.smartcart.cart.repository.CartRepository;
import com.manish.smartcart.order.coupon.service.CouponService;
import com.manish.smartcart.order.dto.PromotionResult;
import com.manish.smartcart.cart.model.Cart;
import com.manish.smartcart.cart.model.CartItem;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.manish.smartcart.sale.model.FlashSaleItem;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.order.coupon.model.Coupon;
import com.manish.smartcart.sale.repository.FlashSaleItemRepository;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.InsufficientStockException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UsersRepository usersRepository;
    private final CouponService couponService;
    private final PromotionEngineService promotionEngine;
    private final FlashSaleItemRepository flashSaleItemRepository;

    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("599.00");
    private static final BigDecimal STANDARD_DELIVERY_FEE = new BigDecimal("50.00");


    @Transactional
    public Cart addItemToCart(Long userId, java.util.UUID variantPublicId, Integer quantity) {
        // 1. Get or Create Cart for User
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> creatNewCart(userId));

        // 2. Find the specific variant the customer selected
        ProductVariant variant = productVariantRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + variantPublicId));
        Long variantId = variant.getId();

        // Guard: Cannot add inactive/delisted variants to cart
        if (!variant.isActive()) {
            throw new BusinessLogicException(
                    "'" + variant.getDisplayLabel() + "' (SKU: " + variant.getSku() + ") is no longer available.");
        }

        // 3. Update existing item or add new one
        CartItem cartItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElse(new CartItem());

        // Validate against TOTAL quantity (existing in cart + new request)
        // This prevents splitting the same SKU across two requests to bypass stock limits.
        int requestedQuantity = (cartItem.getId() == null) ? quantity : cartItem.getQuantity() + quantity;

        // Check available stock = stockQuantity - reservedQuantity (units held by other live carts)
        int available = variant.getAvailableStock();
        if (requestedQuantity >available) {
            throw new InsufficientStockException(
                    "Insufficient stock for: " + variant.getDisplayLabel() +
                            " (SKU: " + variant.getSku() + "). Available: " + available);
        }

        // 4. Set fields or update quantity
        if (cartItem.getId() == null) {
            cartItem.setCart(cart);
            cartItem.setVariant(variant);
            // --- FLASH SALE OVERRIDE INTERCEPTION ---
            // Calculate base standard price first
            BigDecimal basePrice = variant.getProduct().getPrice()
                    .add(variant.getPriceModifier() != null ?  variant.getPriceModifier() : BigDecimal.ZERO);

            // Query if this exact Variant is currently in a LIVE, Admin-Approved Flash Sale
            Optional<FlashSaleItem> activeFlashSale = flashSaleItemRepository.findActiveDiscountForVariant(variant.getId());

            BigDecimal effectivePrice;
            if(activeFlashSale.isPresent()) {

                FlashSaleItem sale = activeFlashSale.get();

                // Validate anti-bot limit: You cannot add more than the Max Units Per User
                if(requestedQuantity > sale.getMaxUnitsPerUser()){
                    throw new BusinessLogicException("Flash Sale Limit: You can only buy "
                            + sale.getMaxUnitsPerUser() + " unit(s) per user.");
                }

                // Apply the discount math: e.g., 20% off -> basePrice * (1 - 0.20)
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(sale.getDiscountPercentage().divide(BigDecimal.valueOf(100)));
                effectivePrice = basePrice.multiply(discountMultiplier);

                log.info("Flash Sale active for SKU {}: Original ₹{}, Discounted to ₹{}", variant.getSku(), basePrice, effectivePrice);
            }else{
                // Standard Pricing
                effectivePrice = basePrice;
            }
            cartItem.setPriceAtAdding(effectivePrice);
            cartItem.setQuantity(quantity);
            // Final add to the list of the cart
            cart.addCartItem(cartItem); // Using helper
        } else {
            // GUARD: Also enforce Flash Sale per-user limit when UPDATING an existing cart item.
            // Without this check, a bot could call addItemToCart 100 times to bypass the limit.
            Optional<FlashSaleItem> activeFlashSaleOnUpdate = flashSaleItemRepository.findActiveDiscountForVariant(variant.getId());
            if (activeFlashSaleOnUpdate.isPresent() && requestedQuantity > activeFlashSaleOnUpdate.get().getMaxUnitsPerUser()) {
                throw new BusinessLogicException("Flash Sale Limit: You can only buy "
                        + activeFlashSaleOnUpdate.get().getMaxUnitsPerUser() + " unit(s) per user.");
            }
            cartItem.setQuantity(requestedQuantity);
        }

        // 4. Recalculate Total
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }

    // Helper method to create a cart
    @Transactional
    protected Cart creatNewCart(Long userId) {
        Users user = usersRepository.getReferenceById(userId);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setDeliveryFee(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    @Transactional
    public void clearTheCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        // 1. Clear the list (JPA orphanRemoval deletes the rows in DB)
        cart.getItems().clear();
        // 2. Reset the total to zero
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setDeliveryFee(BigDecimal.ZERO);
        cart.setCouponCode(null);
        cartRepository.save(cart);
    }

    @Transactional
    public Cart applyCoupon(Long userId, String code) {

        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BusinessLogicException("Cannot apply coupon to an empty cart.");
        }

        // 1. Calculate Gross Subtotal first to pass to validation
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getPriceAtAdding().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Validate and fetch coupon details from DB
        Coupon coupon = couponService.validateCouponForCart(code, userId, subtotal);

        // 3. Temporarily set the coupon on the cart and let the generic Math Engine
        // recalculate everything
        cart.setCouponCode(coupon.getCode());

        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    // Remove a specific item from the cart
    @Transactional
    public Cart removeItemFromCart(Long userId, UUID variantPublicId) {

        Long variantId = cartItemRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Item no found for: #"+ variantPublicId))
                .getId();
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getVariant() != null  && item.getVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variant (ID: " + variantId + ") not found in your cart."));

        cart.removeCartItem(itemToRemove);

        // Vital: Recalculate! If removing this item drops the subtotal below 599,
        // the Delivery Fee will automatically jump back to 50!
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    /**
     * THE MATH ENGINE: This runs every time an item is added, removed, or a coupon is applied.
     * It recalculates the 5-step algebraic pipeline securely and logs its decisions using SLF4J.
     */
    @Transactional
    protected void updateCartTotal(Cart cart) {

        log.debug("Initiating Cart Math Engine for User ID: {}", cart.getUser().getId());

        // Step 1: Gross Subtotal (Sum of quantities * price of all items)
        BigDecimal grossSubTotal = cart.getItems().stream()
                .map(item -> item.getPriceAtAdding().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean forceFreeShipping = false;
        // Step 2: Complex Promotions evaluation
        if(cart.getCouponCode() != null ){
            Coupon coupon = couponService.findActiveCouponByCode(cart.getCouponCode());

            // Validate not just existence, but also specific user exclusivity constraints
            if(coupon != null && coupon.isValidForUser(cart.getUser().getId())) {
                log.info("Valid Coupon [{}] found. Delegating calculation to PromotionEngine...",
                        coupon.getCode());
                // --- THE BRAIN IS CALLED HERE ---
                PromotionResult promoResult =   promotionEngine.evaluate(cart, coupon, grossSubTotal);
                discountAmount = promoResult.discountAmount();
                forceFreeShipping = promoResult.isFreeShippingApplies();

                // Safety net: No massive discounts creating negative carts
                if(discountAmount.compareTo(grossSubTotal) > 0){
                    log.warn("Discount (₹{}) exceeded Gross Total. Capping heavily.", discountAmount);
                    discountAmount = grossSubTotal;
                }

                // If the Engine returned zero money off (e.g., they didn't meet BOGO criteria), strip the code silently\
                if(discountAmount.compareTo(BigDecimal.ZERO) == 0 && !forceFreeShipping){
                    log.warn("Coupon [{}] stripped from cart. Constraints no longer met (Returned ₹0 off).",
                            cart.getCouponCode());

                }
            }else{
                log.warn("Coupon [{}] stripped from cart. Either completely invalid, expired, or locked to another user.",
                        cart.getCouponCode());
                cart.setCouponCode(null);
            }
        }
        // Step 3: Net Subtotal (Total cost of items after discounts are stripped away)
        BigDecimal netSubTotal = grossSubTotal.subtract(discountAmount);
        log.debug("Net Subtotal mathematically calculated as: ₹{}", netSubTotal);

        // Step 4: Delivery Fee Calculation
        BigDecimal deliveryFee = BigDecimal.ZERO;

        // Rule: If they got a FREE_SHIPPING coupon, ignore the rule.
        // Otherwise, if Net Total < 599 -> Automatically charge 50.00 Delivery Fee.
        if (!forceFreeShipping && netSubTotal.compareTo(BigDecimal.ZERO) > 0 && netSubTotal.compareTo(FREE_DELIVERY_THRESHOLD) < 0) {
            log.info("Net Subtotal is below FREE_DELIVERY threshold. Assessing flat ₹50.00 delivery fee.");
            deliveryFee = STANDARD_DELIVERY_FEE;
        }

        // Step 5: Final Checkout Total (Items + Delivery)
        BigDecimal finalTotal = netSubTotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
        log.info("Math Engine completed. Final Total: ₹{}", finalTotal);

        // Persist Snapshot inside the entity state
        cart.setDiscountAmount(discountAmount);
        cart.setDeliveryFee(deliveryFee);
        cart.setTotalAmount(finalTotal);
    }

}
