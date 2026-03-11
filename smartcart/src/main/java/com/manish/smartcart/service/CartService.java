package com.manish.smartcart.service;

import com.manish.smartcart.enums.DiscountType;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.CartItemRepository;
import com.manish.smartcart.repository.CartRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.model.order.Coupon;
import com.manish.smartcart.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;
    private final CouponService couponService;

    // The threshold for Free Delivery
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("599.00");
    // The cost of Delivery if they don't meet the threshold
    private static final BigDecimal STANDARD_DELIVERY_FEE = new BigDecimal("50.00");

    @Transactional
    public Cart addItemToCart(Long userId, Long productId, Integer quantity) {
        // 1. Get or Create Cart for User
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> creatNewCart(userId));

        // 2. Find Product & Check stock
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 3. Update existing item or add new one
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(new CartItem());

        // RECTIFICATION: Validate against TOTAL quantity (Current in Cart + New
        // Request)
        int requestedQuantity = (cartItem.getId() == null) ? quantity : cartItem.getQuantity() + quantity;
        if (requestedQuantity > product.getStockQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        // 4. check if are getting else add to the new one
        if (cartItem.getId() == null) {
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setPriceAtAdding(product.getPrice());
            cartItem.setQuantity(quantity);
            // Final add to the list of the cart
            cart.addCartItem(cartItem); // Using helper
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        // 4. Recalculate Total
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }

    // Helper method to create a cart
    private Cart creatNewCart(Long userId) {
        Users user = usersRepository.getReferenceById(userId);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setDeliveryFee(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }

    @Transactional
    public void clearTheCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found"));
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

        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot apply coupon to an empty cart.");
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

    /**
     * THE MATH ENGINE: This runs every time an item is added, removed, or a coupon
     * is applied.
     * It recalculates the 5-step pipeline securely.
     */
    private void updateCartTotal(Cart cart) {

        // Step 1: Gross Subtotal
        BigDecimal grossSubTotal = cart.getItems().stream()
                .map(item -> item.getPriceAtAdding().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;

        // Step 2: Handle Discount — safe coupon check (no exception thrown inside TX)
        if (cart.getCouponCode() != null) {
            // Use a non-throwing lookup so the transaction is never marked rollback-only
            Coupon coupon = couponService.findActiveCouponByCode(cart.getCouponCode());

            boolean couponStillValid = coupon != null
                    && coupon.getIsActive()
                    && (coupon.getMinOrderAmount() == null
                            || grossSubTotal.compareTo(coupon.getMinOrderAmount()) >= 0);

            if (couponStillValid) {
                if (coupon.getDiscountType() == DiscountType.FLAT) {
                    discountAmount = coupon.getDiscountValue();
                } else {
                    // Percentage Logic
                    BigDecimal discountFactor = coupon.getDiscountValue().divide(new BigDecimal("100"), 2,
                            RoundingMode.HALF_UP);
                    discountAmount = grossSubTotal.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

                    if (coupon.getMaxDiscountAmount() != null
                            && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                        discountAmount = coupon.getMaxDiscountAmount();
                    }
                }
                // Make sure discount doesn't exceed the subtotal itself
                if (discountAmount.compareTo(grossSubTotal) > 0) {
                    discountAmount = grossSubTotal;
                }
            } else {
                // Coupon is no longer valid (expired, min-order no longer met, etc.) — strip it
                // silently
                log.warn("Coupon '{}' stripped from cart (no longer valid for subtotal {})",
                        cart.getCouponCode(), grossSubTotal);
                cart.setCouponCode(null);
                discountAmount = BigDecimal.ZERO;
            }
        }

        // Step 3: Net Subtotal
        BigDecimal netSubTotal = grossSubTotal.subtract(discountAmount);

        // Step 4: Delivery Fee Calculation
        BigDecimal deliveryFee = BigDecimal.ZERO;

        // If Net Subtotal is greater than 0 BUT less than 599, charge Delivery Fee!
        if (netSubTotal.compareTo(BigDecimal.ZERO) > 0 && netSubTotal.compareTo(FREE_DELIVERY_THRESHOLD) < 0) {
            deliveryFee = STANDARD_DELIVERY_FEE;
        }

        // Step 5: Final Total
        BigDecimal finalTotal = netSubTotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        // Save Snapshot
        cart.setDiscountAmount(discountAmount);
        cart.setDeliveryFee(deliveryFee);
        cart.setTotalAmount(finalTotal);
    }

    // Remove a specific item from the cart
    @Transactional
    public Cart removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not in the cart"));

        cart.removeCartItem(itemToRemove);

        // Vital: Recalculate! If removing this item drops the subtotal below 599,
        // the Delivery Fee will automatically jump back to 50!
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

}
