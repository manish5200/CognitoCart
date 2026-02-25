package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.WishlistSummaryDTO;
import com.manish.smartcart.service.WishlistService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "9. Wishlist Management", description = "Endpoints for saving and managing favorite products")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

        private final WishlistService wishlistService;

        public WishlistController(WishlistService wishlistService) {
                this.wishlistService = wishlistService;
        }

        @Operation(summary = "Toggle Wishlist Item", description = "Adds a product to the wishlist if it's not present, or removes it if it already exists.")
        @ApiResponse(responseCode = "200", description = "Wishlist updated successfully")
        @PostMapping("/toggle/{productId}")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<?> toggleWishlist(@PathVariable("productId") Long productId,
                        Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                assert customUserDetails != null;
                Long userId = customUserDetails.getUser().getId();
                String message = wishlistService.toggleWishlist(userId, productId);
                return ResponseEntity.ok().body(Map.of("Status", message));
        }

        @Operation(summary = "Get My Wishlist", description = "Retrieves all products currently saved in the user's wishlist.")
        @GetMapping
        public ResponseEntity<List<ProductResponse>> getMyWishlist(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                List<ProductResponse> wishlist = wishlistService.getWishlistForUser(userDetails.getUser().getId());
                return ResponseEntity.ok(wishlist);
        }

        @Operation(summary = "Move Item to Cart", description = "Adds a wishlisted product to the cart and removes it from the wishlist.")
        @PostMapping("/move-to-cart/{productId}")
        public ResponseEntity<?> moveToCart(
                        @PathVariable("productId") Long productId,
                        @RequestParam(defaultValue = AppConstants.PRODUCT_QUANTITY) Integer quantity,
                        Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                assert customUserDetails != null;
                Long userId = customUserDetails.getUser().getId();
                CartResponse cartResponse = wishlistService.wishlistToCart(userId, productId, quantity);
                return ResponseEntity.ok(Map.of("Item moved to cart successfully", cartResponse));
        }

        @Operation(summary = "Get Wishlist Summary", description = "Returns all wishlisted items with a calculated total value.")
        @GetMapping("/summary")
        public ResponseEntity<?> getWishlistSummary(Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                assert customUserDetails != null;
                Long userId = customUserDetails.getUser().getId();
                WishlistSummaryDTO wishlistSummaryDTO = wishlistService.getWishlistSummary(userId);
                return ResponseEntity.ok(Map.of("Wishlist Summary", wishlistSummaryDTO));
        }
}
