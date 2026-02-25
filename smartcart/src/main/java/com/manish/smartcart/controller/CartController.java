package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartRequest;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.service.CartService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "5. Cart Management", description = "Operations related to managing the shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

        private final CartService cartService;

        public CartController(CartService cartService) {
                this.cartService = cartService;
        }

        // POST: Add item to cart
        // Request Body: { "productId": 1, "quantity": 2 }
        @Operation(summary = "Add item to cart", description = "Adds a product to the user's cart or updates quantity if already present.")
        @PostMapping("/add")
        public ResponseEntity<?> addItemToCart(@RequestBody @Valid CartRequest cartRequest,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Cart updatedCart = cartService.addItemToCart(
                                userDetails.getUser().getId(),
                                cartRequest.getProductId(),
                                cartRequest.getQuantity());
                CartResponse cartResponse = new CartResponse().getCartResponse(updatedCart);
                return ResponseEntity.ok().body(Map.of("Cart updated :", cartResponse));
        }

        // Cart Summary
        @Operation(summary = "Get cart summary", description = "Retrieves all items in the current user's cart including subtotal and totals.")
        @GetMapping("/summary")
        public ResponseEntity<?> getCartSummary(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Cart cart = cartService.getCartForUser(userDetails.getUser().getId());
                CartResponse cartResponse = new CartResponse().getCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);

        }

        @Operation(summary = "Clear cart", description = "Removes all items from the current user's shopping cart.")
        @DeleteMapping("/clear")
        public ResponseEntity<?> clearCart(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();
                cartService.clearTheCart(userId);
                return ResponseEntity.ok().body(Map.of("message", "Cart cleared successfully✅"));
        }

        @Operation(summary = "Apply discount coupon", description = "Applies a percentage-based discount to the cart total.")
        @PostMapping("/apply-coupon")
        public ResponseEntity<?> applyCoupon(
                        @RequestParam @Min(value = (long) AppConstants.MINIMUM_COUPON_DISCOUNT) @Max(value = (long) AppConstants.MAXIMUM_COUPON_DISCOUNT) Double percentage,
                        Authentication authentication) {

                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();
                Cart cart = cartService.applyCoupon(userId, percentage);

                CartResponse cartResponse = new CartResponse().getCartResponse(cart);

                return ResponseEntity.ok().body(cartResponse);
        }

        @Operation(summary = "Remove item from cart", description = "Deletes a specific product entry from the user's cart.")
        @DeleteMapping("/item/{productId}")
        public ResponseEntity<?> deleteItemFromCart(@PathVariable("productId") Long productId, Authentication auth) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();
                Cart cart = cartService.removeItemFromCart(userId, productId);
                CartResponse cartResponse = new CartResponse().getCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);
        }
}
