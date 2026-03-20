package com.manish.smartcart.service;

import com.manish.smartcart.model.cart.GuestCart;
import com.manish.smartcart.model.cart.GuestCartItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.GuestCartRepository;
import com.manish.smartcart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCartService {

     private final GuestCartRepository guestCartRepository;
     private final ProductRepository productRepository; // Still need Postgres to verify prices/stock!
     private final CartService cartService;

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
    public GuestCart addItem(String sessionId, Long productId, Integer quantity){
        GuestCart cart = getCart(sessionId);

        // 1. Validate Product exists in Postgres and has stock
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 2. See if the item is already in the Redis cart
        Optional<GuestCartItem>existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst();

        if(existingItemOpt.isPresent()){
            GuestCartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;

            if (newQuantity > product.getStockQuantity()) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            // Update price in case it changed since they last looked
            existingItem.setPriceAtAdding(product.getPrice());
        }else {

            if (quantity > product.getStockQuantity()) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            GuestCartItem newItem = new GuestCartItem(
                    productId, quantity, product.getPrice());
            cart.getItems().add(newItem);
        }

        // 3. Save back to Redis
        return guestCartRepository.save(cart);
    }

    /**
     * Removes a specific item from the Redis cart.
     */
    public GuestCart removeItem(String sessionId, Long productId){
        GuestCart cart = getCart(sessionId);
        boolean removed = cart.getItems()
                .removeIf(item ->
                        item.getProductId().equals(productId));

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
                    cartService.addItemToCart(userId, item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to merge product {} for user {}: {}",
                            item.getProductId(), userId, e.getMessage());
                }
            }
            // Obliterate the temporary Redis cart so it isn't orphaned!
            guestCartRepository.deleteById(sessionId);
            log.info("✅ Merge complete. Guest Cart [{}] deleted.", sessionId);
        });
    }

}
