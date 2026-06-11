package com.manish.smartcart.service;

import com.manish.smartcart.model.cart.GuestCart;
import com.manish.smartcart.model.cart.GuestCartItem;
import com.manish.smartcart.model.product.ProductVariant;
import com.manish.smartcart.repository.GuestCartRepository;
import com.manish.smartcart.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCartService {

     private final GuestCartRepository guestCartRepository;
     private final CartService cartService;
     private final ProductVariantRepository productVariantRepository;

    /**
     * Finds the cart in Redis by Session ID. If it doesn't exist, we create an empty one.
     */
    public GuestCart getCart(String sessionId){
        return guestCartRepository.findById(sessionId)
                .orElseGet(()->{
                    GuestCart newGuestCart = new GuestCart();
                    newGuestCart.setSessionId(sessionId);
                    // newCart.setTimeToLive(7L); // set by default in entity
                    return newGuestCart;
                });
    }

    /**
     * Adds an item to the Redis Cart.
     */
    public GuestCart addItem(String sessionId, Long variantId, Integer quantity){
        GuestCart cart = getCart(sessionId);

        // 1. Validate variant exists in Postgres and has available stock
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));

        int available = variant.getAvailableStock();

        // 2. See if this variant is already in the Redis cart
        Optional<GuestCartItem>existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getVariantId().equals(variantId))
                .findFirst();

        if(existingItemOpt.isPresent()){
            GuestCartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;

            if (newQuantity > available) {
                throw new RuntimeException("Insufficient stock. Available: " + available);
            }
            existingItem.setQuantity(newQuantity);
            // Refresh effective price (base + modifier) in case it changed
            BigDecimal effectivePrice = variant.getProduct().getPrice()
                    .add(variant.getPriceModifier() != null
                            ? variant.getPriceModifier()
                            : BigDecimal.ZERO);

            // Update price in case it changed since they last looked
            existingItem.setPriceAtAdding(effectivePrice);
        }else {

            if (quantity > available) {
                throw new RuntimeException("Insufficient stock. Available: " + available);
            }

            BigDecimal effectivePrice = variant.getProduct().getPrice()
                    .add(variant.getPriceModifier() != null
                            ? variant.getPriceModifier()
                            : BigDecimal.ZERO);
            GuestCartItem newItem = new GuestCartItem(
                    variantId, quantity, effectivePrice);
            cart.getItems().add(newItem);
        }

        // 3. Save back to Redis
        return guestCartRepository.save(cart);
    }

    /**
     * Removes a specific item from the Redis cart.
     */
    public GuestCart removeItem(String sessionId, Long variantId){
        GuestCart cart = getCart(sessionId);
        boolean removed = cart.getItems()
                .removeIf(item ->
                        item.getVariantId().equals(variantId));

        if (!removed) {
            throw new RuntimeException("Item not found in guest cart");
        }
        return guestCartRepository.save(cart);
    }

    /**
     * Clears the entire cart from Redis.
     */
    public void deleteCart(String sessionId) {
        guestCartRepository.deleteById(sessionId);
    }

    /**
     * Merges the anonymous Redis cart into the permanent PostgreSQL cart.
     */
    public void mergeCart(String sessionId, Long userId){
        guestCartRepository.findById(sessionId).ifPresent(guestCart -> {
            log.info("🛒 Merging Guest Cart [{}] into User [{}]", sessionId, userId);

            for(GuestCartItem item : guestCart.getItems()) {
                try {
                    // Call the secure Postgres service to handle stock validation and Math Engine recalcs
                    cartService.addItemToCart(userId, item.getVariantId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to merge product {} for user {}: {}",
                            item.getVariantId(), userId, e.getMessage());
                }
            }
            // Obliterate the temporary Redis cart so it isn't orphaned!
            guestCartRepository.deleteById(sessionId);
            log.info("✅ Merge complete. Guest Cart [{}] deleted.", sessionId);
        });
    }

}
