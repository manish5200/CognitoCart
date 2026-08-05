package com.manish.smartcart.wishlist.controller;

import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.cart.dto.CartResponse;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.wishlist.dto.WishlistSummaryDTO;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.wishlist.service.WishlistService;
import com.manish.smartcart.shared.util.AppConstants;
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
import java.util.UUID;

/**
 * Edge API Gateway for Wishlist Operations.
 * <p>
 * ARCHITECTURE NOTE (The Translator Pattern):
 * Injecting Repositories into Controllers is typically an anti-pattern. However, we employ it here
 * strictly for "Edge Translation". External clients only know the public UUID (preventing IDOR).
 * The Controller translates this UUID into the internal Long PK before passing it to the core
 * domain Service. This keeps the Service layer completely decoupled from external presentation logic.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist Management", description = "Endpoints for saving and managing favorite products")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

        private final WishlistService wishlistService;

        /**
         * Idempotent state toggle for wishlist items.
         * Safe for frontend clients to blindly call on rapid successive clicks without causing
         * duplicate database entries or throwing constraint violations.
         */
        @Operation(summary = "Toggle Wishlist Item",
                description = "Idempotent toggle — adds the product if not in wishlist, removes it if already saved.")
        @ApiResponse(responseCode = "200", description = "Wishlist updated successfully")
        @PostMapping("/toggle/{productPublicId}")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<?> toggleWishlist(@PathVariable UUID productPublicId,
                                                Authentication authentication) {
                Long userId = extractUserId(authentication);
                String message = wishlistService.toggleWishlist(userId, productPublicId);
                return ResponseEntity.ok().body(Map.of("Status", message));
        }

        /**
         * Retrieves the user's active wishlist.
         * NOTE: As the platform scales, consider paginating this endpoint if telemetry shows
         * users hoarding 100+ items, which would cause heavy JSON serialization overhead.
         */
        @Operation(summary = "Get My Wishlist",
                description = "Returns all products currently saved in the user's wishlist as full product cards.")
        @ApiResponse(responseCode = "200", description = "Successfully retrieved wishlist items")
        @GetMapping
        public ResponseEntity<List<ProductResponse>> getMyWishlist(Authentication authentication) {
                Long userId = extractUserId(authentication);
                List<ProductResponse> wishlist = wishlistService.getWishlistForUser(userId);
                return ResponseEntity.ok(wishlist);
        }

        /**
         * State Transition Pipeline: Wishlist -> Cart.
         * Requires atomicity in the service layer to ensure the item is not stranded in both
         * domains if a database constraint fails during the cart insertion phase.
         */
        @Operation(summary = "Move Item to Cart",
                description = "Adds a wishlisted product to the cart and removes it from the wishlist.")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Item moved to cart successfully"),
                @ApiResponse(responseCode = "404", description = "Product not found in wishlist")
        })
        @PostMapping("/move-to-cart/{productPublicId}")
        public ResponseEntity<?> moveToCart(
                @PathVariable UUID productPublicId,
                @RequestParam(name = "quantity", defaultValue = AppConstants.PRODUCT_QUANTITY) Integer quantity,
                Authentication authentication) {
                Long userId = extractUserId(authentication);
                CartResponse cartResponse = wishlistService.wishlistToCart(userId, productPublicId, quantity);
                return ResponseEntity.ok(Map.of("Item moved to cart successfully", cartResponse));
        }

        /**
         * Dynamic aggregation of wishlist financial metrics.
         * WARNING: This executes on-the-fly calculations. If wishlist sizes grow, this should
         * be offloaded to a materialized view or cached via Redis.
         */
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


        /**
         * Context Extractor.
         * Fails fast if the SecurityContext is compromised or improperly hydrated by the JWT Filter.
         */
        private Long extractUserId(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                if (userDetails == null) {
                        throw new BusinessLogicException("Authentication context is missing. Please log in again.");
                }
                return userDetails.getUser().getId();
        }

}
