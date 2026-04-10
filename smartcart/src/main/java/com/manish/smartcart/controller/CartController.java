package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartRequest;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.service.CartService;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart Management", description = "Operations related to managing the shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

        private final CartService cartService;

        private long extractUserId(Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                if(customUserDetails == null){
                        throw new BusinessLogicException("Authentication context is missing. Please log in again.");
                }
                return customUserDetails.getUser().getId();
        }

        // POST: Add item to cart
        // Request Body: { "productId": 1, "quantity": 2 }
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
                                cartRequest.getProductId(),
                                cartRequest.getQuantity());
                CartResponse cartResponse = new CartResponse().getCartResponse(updatedCart);
                return ResponseEntity.ok().body(Map.of("Cart updated :", cartResponse));
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
                CartResponse cartResponse = new CartResponse().getCartResponse(cart);
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

                CartResponse cartResponse = new CartResponse().getCartResponse(cart);

                return ResponseEntity.ok().body(cartResponse);
        }


        @Operation(summary = "Remove item from cart", description = "Deletes a specific product entry from the user's cart.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found in cart")
        })
        @DeleteMapping("/item/{productId}")
        public ResponseEntity<?> deleteItemFromCart(@PathVariable Long productId,
                        Authentication auth) {
                Long userId = extractUserId(auth);
                Cart cart = cartService.removeItemFromCart(userId, productId);
                CartResponse cartResponse = new CartResponse().getCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);
        }
}
