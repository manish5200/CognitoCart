package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.cart.CartRequest;
import com.manish.smartcart.dto.cart.CartResponse;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?>addItemToCart(@RequestBody CartRequest cartRequest, Authentication authentication){
          try{
              CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
              Cart updatedCart = cartService.addItemToCart(
                      userDetails.getUserId(),
                      cartRequest.getProductId(),
                      cartRequest.getQuantity()
              );
              CartResponse cartResponse = new CartResponse().getCartResponse(updatedCart);
              return ResponseEntity.ok().body(Map.of("Cart updated :",cartResponse));

          }catch (Exception e){
              return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
          }
    }

    //Cart Summary
    @GetMapping("/summary")
    public ResponseEntity<?> getCartSummary(Authentication authentication){
           try{
               CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Cart cart = cartService.getCartForUser(userDetails.getUserId());
                CartResponse cartResponse = new CartResponse().getCartResponse(cart);
                return ResponseEntity.ok().body(cartResponse);
           }catch (Exception e){
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
           }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication authentication){
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            cartService.clearTheCart(userId);
            return  ResponseEntity.ok().body(Map.of("message","Cart cleared successfully✅"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
        }
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<?> applyCoupon(@RequestParam Double percentage, Authentication authentication){
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();

            if(percentage < 0 || percentage > 100){
                throw new Exception("Invalid percentage");
            }

            Cart cart = cartService.applyCoupon(userId, percentage);

            CartResponse cartResponse = new CartResponse().getCartResponse(cart);

            return ResponseEntity.ok().body(cartResponse);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
        }
    }

}
