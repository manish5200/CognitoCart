package com.manish.smartcart.wishlist.service;

import com.manish.smartcart.cart.dto.CartResponse;
import com.manish.smartcart.cart.mapper.CartMapper;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.product.repository.ProductRepository;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.manish.smartcart.user.repository.UsersRepository;
import com.manish.smartcart.wishlist.dto.WishlistSummaryDTO;
import com.manish.smartcart.shared.mapper.ProductMapper;
import com.manish.smartcart.cart.model.Cart;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.wishlist.model.Wishlist;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.cart.service.CartService;
import com.manish.smartcart.wishlist.repository.WishlistRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Domain Service for Wishlist Orchestration.
 * <p>
 * ARCHITECTURAL DESIGN:
 * 1. Transaction Boundary: This layer exclusively owns the @Transactional context.
 * 2. Edge Translation: It safely translates external API tokens (UUIDs) into internal surrogate keys (Long).
 * 3. Cross-Domain Orchestration: Manages atomic state transfers between the Wishlist and Cart domains.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UsersRepository usersRepository;
    private final ProductMapper productMapper;
    private final CartService cartService;
    private final CartMapper cartMapper;

    /**
     * Idempotent Wishlist Toggle.
     * <p>
     * PERFORMANCE SAFEGUARDS:
     * - Single Query Translation: Fetches the Product entity via UUID in one DB hit.
     * - Proxy Hydration: Uses usersRepository.getReferenceById() to create a Hibernate proxy
     *   for the User entity. This allows us to set the Foreign Key on the Wishlist object
     *   WITHOUT executing an expensive SELECT query against the Users table.
     */
    @Transactional
    public String toggleWishlist(Long userId, UUID productPublicId) {

        // 1. Edge Translation & Entity Retrieval
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for UUID: " + productPublicId));

        // 2. Idempotent Toggle Logic
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, product.getId());

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return "Product Removed from Wishlist";
        } else {
            Wishlist wishlist = new Wishlist();

            // Assigning FK via Proxy (Bypasses a DB SELECT)
            wishlist.setUser(usersRepository.getReferenceById(userId));
            wishlist.setProduct(product);

            wishlistRepository.save(wishlist);
            return "Product Added to Wishlist";
        }
    }

    /**
     * Read-Only State Retrieval.
     * <p>
     * DB OPTIMIZATION: @Transactional(readOnly = true) is strictly enforced to disable
     * Hibernate's dirty-checking mechanism and avoid unnecessary flush cycles, saving memory.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlistForUser(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(item -> productMapper.toProductResponse(item.getProduct()))
                .toList();
    }

    /**
     * Atomic Domain Transfer: Wishlist -> Cart.
     * <p>
     * This acts as a synchronous Micro-Saga. If the CartService fails (e.g., Out of Stock,
     * Math Engine exception), the entire transaction rolls back. The item will NOT be
     * accidentally deleted from the wishlist.
     */
    @Transactional
    public CartResponse wishlistToCart(Long userId, UUID productPublicId, Integer quantity) {

        // 1. Edge Translation (Only fetching the internal Long ID)
        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for UUID: " + productPublicId))
                .getId();

        // 2. Verify existence in Wishlist
        Wishlist existedProductInWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in your wishlist"));

        // 3. Database-Level Variant Resolution
        // CRITICAL: We push the sorting logic to the database (ORDER BY sort_order LIMIT 1).
        // Pulling all variants into application memory to do stream().min() causes heap exhaustion on large catalogs.
        ProductVariant defaultVariant = productVariantRepository
                .findFirstByProductIdAndIsActiveTrueOrderBySortOrderAsc(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active variant available for product ID: " + productId + ". The product may be out of stock."));

        // 4. Cross-Domain Orchestration: Add to Cart (Handles inventory checks + pricing)
        Cart cart = cartService.addItemToCart(userId, defaultVariant.getPublicId(), quantity);

        // 5. Complete Domain Transfer
        wishlistRepository.delete(existedProductInWishlist);

        return cartMapper.toCartResponse(cart);
    }

    /**
     * Financial Aggregation of Wishlist State.
     * <p>
     * NOTE: Computations are handled in-memory within the JVM via Streams. Since standard e-commerce
     * wishlists rarely exceed 50-100 items per user, this is CPU-efficient. If metrics show users hoarding
     * 1000+ items, shift this calculation to a database aggregate query (SUM).
     */
    @Transactional(readOnly = true)
    public WishlistSummaryDTO getWishlistSummary(Long userId) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(userId);

        List<ProductResponse> productResponses = wishlistItems.stream()
                .map(item -> productMapper.toProductResponse(item.getProduct()))
                .toList();

        // In-Memory Big-Decimal Aggregation
        BigDecimal totalValue = wishlistItems.stream()
                .map(item -> item.getProduct().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new WishlistSummaryDTO(productResponses, productResponses.size(), totalValue);
    }
}
