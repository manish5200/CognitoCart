package com.manish.smartcart.service;

import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.WishlistSummaryDTO;
import com.manish.smartcart.mapper.ProductMapper;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.Wishlist;
import com.manish.smartcart.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;
    private final ProductMapper  productMapper;
    private final CartService cartService;
    public WishlistService(WishlistRepository wishlistRepository,
                           ProductRepository productRepository,
                           UsersRepository usersRepository,
                           ProductMapper productMapper,
                           CartService cartService) {
         this.wishlistRepository = wishlistRepository;
         this.productRepository = productRepository;
         this.usersRepository = usersRepository;
         this.productMapper = productMapper;
         this.cartService = cartService;
    }

    @Transactional
    public String toggleWishlist(Long userId, Long productId) {

        // 1. Actually verify the product exists first
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " not found"));

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if(existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return "Product Removed from Wishlist";
        }else{
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(usersRepository.getReferenceById(userId));
            wishlist.setProduct(product);
            wishlistRepository.save(wishlist);
            return "Product Added to Wishlist";
        }
    }


    public List<ProductResponse>getWishlistForUser(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
        return wishlists.stream()
                .map(item -> productMapper.toProductResponse(item.getProduct()))// Convert to DTO
                .toList();
    }


    //wishlist -> cart
    @Transactional
    public CartResponse wishlistToCart(Long userId, Long productId, Integer quantity) {
        // 1. Verify item exists in Wishlist
       Wishlist existedProductInWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
               .orElseThrow(() -> new RuntimeException("Item not found in your wishlist"));

        // 2. Use existing CartService to add to cart
        // This ensures cart totals and stock checks are handled correctly
        Cart cart = cartService.addItemToCart(userId, productId, quantity);

        // 3. Remove from Wishlist
        wishlistRepository.delete(existedProductInWishlist);

        return new CartResponse().getCartResponse(cart);

    }

    public WishlistSummaryDTO getWishlistSummary(Long userId) {
        List<Wishlist>wishlistItems = wishlistRepository.findByUserId(userId);
        // 1. Convert to ProductResponse
        List<ProductResponse>productResponses = wishlistItems.stream()
                .map(item -> productMapper.toProductResponse(item.getProduct()))
                .toList();
        // 2. Calculate Total Value of all wishlisted items
        BigDecimal totalValue = wishlistItems.stream()
                .map(item -> item.getProduct().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WishlistSummaryDTO(
                productResponses, productResponses.size(),  totalValue
        );
    }

}
