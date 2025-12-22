package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest orderRequest, Authentication authentication) {
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            OrderResponse orderResponse = orderService.placeOrder(userId,orderRequest);
            return ResponseEntity.status(HttpStatus.OK).body(orderResponse);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/history")
    public ResponseEntity<?> getOrderHistory(Authentication authentication) {
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();

            List<OrderResponse> history = orderService.getOrderHistoryForUser(userId);
            return ResponseEntity.ok(history);

        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not retrieve order history"));
        }
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            OrderResponse orderResponse = orderService.cancelOrder(userId,orderId);
            return ResponseEntity.ok(orderResponse);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
