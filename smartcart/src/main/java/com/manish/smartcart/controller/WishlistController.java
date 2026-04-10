package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.WishlistSummaryDTO;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.WishlistService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist Management", description = "Endpoints for saving and managing favorite products")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

        private final WishlistService wishlistService;


        @Operation(summary = "Toggle Wishlist Item",
                description = "Idempotent toggle — adds the product if not in wishlist, removes it if already saved. " +
                        "Returns a message indicating the action taken.")
        @ApiResponse(responseCode = "200", description = "Wishlist updated successfully")
        @PostMapping("/toggle/{productId}")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<?> toggleWishlist(@PathVariable Long productId,
                                                Authentication authentication) {
                Long userId = extractUserId(authentication);
                String message = wishlistService.toggleWishlist(userId, productId);
                return ResponseEntity.ok().body(Map.of("Status", message));
        }

        @Operation(summary = "Get My Wishlist",
                description = "Returns all products currently saved in the user's wishlist as full product cards.")
        @ApiResponse(responseCode = "200", description = "Successfully retrieved wishlist items")
        @GetMapping
        public ResponseEntity<List<ProductResponse>> getMyWishlist(Authentication authentication) {
                Long userId = extractUserId(authentication);
                List<ProductResponse> wishlist = wishlistService.getWishlistForUser(userId);
                return ResponseEntity.ok(wishlist);
        }

        @Operation(summary = "Move Item to Cart",
                description = "Adds a wishlisted product to the cart and removes it from the wishlist.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item moved to cart successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found in wishlist")
        })
        @PostMapping("/move-to-cart/{productId}")
        public ResponseEntity<?> moveToCart(
                @PathVariable Long productId,
                        @RequestParam(name = "quantity", defaultValue = AppConstants.PRODUCT_QUANTITY) Integer quantity,
                        Authentication authentication) {
                Long userId = extractUserId(authentication);
                CartResponse cartResponse = wishlistService.wishlistToCart(userId, productId, quantity);
                return ResponseEntity.ok(Map.of("Item moved to cart successfully", cartResponse));
        }

        @Operation(
                summary = "Get Wishlist Summary",
                description = "Returns all wishlisted items with a calculated total value."
        )
        @ApiResponse(responseCode = "200", description = "Successfully retrieved wishlist summary")
        @GetMapping("/summary")
        public ResponseEntity<?> getWishlistSummary(Authentication authentication) {
                Long userId = extractUserId(authentication);
                WishlistSummaryDTO wishlistSummaryDTO = wishlistService.getWishlistSummary(userId);
                return ResponseEntity.ok(Map.of("Wishlist Summary", wishlistSummaryDTO));
        }


        private Long extractUserId(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                if (userDetails == null) {
                        throw new BusinessLogicException("Authentication context is missing. Please log in again.");
                }
                return userDetails.getUser().getId();
        }

}
