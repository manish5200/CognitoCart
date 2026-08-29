package com.manish.smartcart.cart.controller;

import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.cart.dto.CartRequest;
import com.manish.smartcart.cart.dto.CartResponse;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.cart.model.Cart;
import com.manish.smartcart.cart.service.CartService;
import com.manish.smartcart.cart.mapper.CartMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart Management", description = "Operations related to managing the shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

        private final CartService cartService;
        private final CartMapper cartMapper;

        // POST: Add item to cart
        // Request Body: { "variantId": 5, "quantity": 2 }
        @Operation(summary = "Add item to cart", description = "Adds a product to the user's cart or updates quantity if already present.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added successfully"),
            @ApiResponse(responseCode = "404", description = "User or Product not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock for requested quantity")
        })
        @PostMapping("/add")
        public ResponseEntity<?> addItemToCart(@RequestBody @Valid CartRequest cartRequest,
                        Authentication authentication) {
                Long userId = extractUserId(authentication);
                Cart updatedCart = cartService.addItemToCart(
                                userId,
                                cartRequest.getVariantPublicId(),
                                cartRequest.getQuantity());
                CartResponse cartResponse = cartMapper.toCartResponse(updatedCart);
                return ResponseEntity.ok().body(Map.of("Cart updated :", cartResponse));
        }

        // PUT: Update item quantity in cart
        @Operation(summary = "Update item quantity", description = "Updates the quantity of a product in the user's cart.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item updated successfully"),
            @ApiResponse(responseCode = "404", description = "User or Item not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock for requested quantity")
        })
        @PutMapping("/item/{variantPublicId}")
        public ResponseEntity<?> updateItemQuantity(@PathVariable java.util.UUID variantPublicId,
                        @RequestParam("quantity") int quantity,
                        Authentication authentication) {
                Long userId = extractUserId(authentication);
                Cart updatedCart = cartService.updateItemQuantity(userId, variantPublicId, quantity);
                CartResponse cartResponse = cartMapper.toCartResponse(updatedCart);
                return ResponseEntity.ok().body(cartResponse);
        }


        // Cart Summary
        @Operation(summary = "Get cart summary", description = "Retrieves all items in the current user's cart including subtotal and totals.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart summary retrieved"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
        })
        @GetMapping("/summary")
        public ResponseEntity<?> getCartSummary(Authentication authentication) {
                Long userId = extractUserId(authentication);
                Cart cart = cartService.getCartForUser(userId);
                CartResponse cartResponse = cartMapper.toCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);

        }


        @Operation(summary = "Clear cart", description = "Removes all items from the current user's shopping cart.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
        })
        @DeleteMapping("/clear")
        public ResponseEntity<?> clearCart(Authentication authentication) {
                Long userId = extractUserId(authentication);
                cartService.clearTheCart(userId);
                return ResponseEntity.ok().body(Map.of("message", "Cart cleared successfully✅"));
        }


        @Operation(summary = "Apply discount coupon", description = "Applies a coupon code to the cart total.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid or expired coupon code")
        })
        @PostMapping("/apply-coupon")
        public ResponseEntity<?> applyCoupon(
                        @RequestParam("code") String code,
                        Authentication authentication) {
                Long userId = extractUserId(authentication);
                Cart cart = cartService.applyCoupon(userId, code);

                CartResponse cartResponse = cartMapper.toCartResponse(cart);

                return ResponseEntity.ok().body(cartResponse);
        }


        @Operation(summary = "Remove item from cart", description = "Deletes a specific product entry from the user's cart.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found in cart")
        })
        @DeleteMapping("/item/{variantPublicId}")
        public ResponseEntity<?> deleteItemFromCart(@PathVariable UUID variantPublicId,
                        Authentication auth) {
                Long userId = extractUserId(auth);
                Cart cart = cartService.removeItemFromCart(userId, variantPublicId);
                CartResponse cartResponse = cartMapper.toCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);
        }

        /**
         * Hardened Security Extractor.
         * Prevents ClassCastException if the user is unauthenticated or passing a malformed token.
         */
        private long extractUserId(Authentication authentication) {
                if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
                        throw new BusinessLogicException("Authentication context is missing or invalid. Please log in again.");
                }
                return userDetails.getUser().getId();
        }

}
