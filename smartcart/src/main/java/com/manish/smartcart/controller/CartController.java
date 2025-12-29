package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartRequest;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // POST: Add item to cart
    // Request Body: { "productId": 1, "quantity": 2 }
    @PostMapping("/add")
    public ResponseEntity<?>addItemToCart(@RequestBody @Valid CartRequest cartRequest, Authentication authentication){
              CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
              Cart updatedCart = cartService.addItemToCart(
                      userDetails.getUserId(),
                      cartRequest.getProductId(),
                      cartRequest.getQuantity()
              );
              CartResponse cartResponse = new CartResponse().getCartResponse(updatedCart);
              return ResponseEntity.ok().body(Map.of("Cart updated :",cartResponse));
    }

    //Cart Summary
    @GetMapping("/summary")
    public ResponseEntity<?> getCartSummary(Authentication authentication){
           CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Cart cart = cartService.getCartForUser(userDetails.getUserId());
            CartResponse cartResponse = new CartResponse().getCartResponse(cart);
            return ResponseEntity.ok().body(cartResponse);

    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication authentication){
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            cartService.clearTheCart(userId);
            return  ResponseEntity.ok().body(Map.of("message","Cart cleared successfully✅"));
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<?> applyCoupon(@RequestParam @Min(value = 0) @Max(value = 100) Double percentage, Authentication authentication){

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            Cart cart = cartService.applyCoupon(userId, percentage);

            CartResponse cartResponse = new CartResponse().getCartResponse(cart);

            return ResponseEntity.ok().body(cartResponse);
    }


    @DeleteMapping("/item/{productId}")
    public ResponseEntity<?> deleteItemFromCart(@PathVariable("productId") Long productId, Authentication auth){
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long userId = userDetails.getUserId();
            Cart cart = cartService.removeItemFromCart(userId, productId);
            CartResponse cartResponse = new CartResponse().getCartResponse(cart);
            return ResponseEntity.ok().body(cartResponse);
    }
}
