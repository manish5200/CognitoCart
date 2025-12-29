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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest orderRequest, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            OrderResponse orderResponse = orderService.placeOrder(userId,orderRequest);
            return ResponseEntity.status(HttpStatus.OK).body(orderResponse);
    }


    @GetMapping("/history")
    public ResponseEntity<?> getOrderHistory(Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();

            List<OrderResponse> history = orderService.getOrderHistoryForUser(userId);
            return ResponseEntity.ok(history);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            OrderResponse orderResponse = orderService.cancelOrder(userId,orderId);
            return ResponseEntity.ok(orderResponse);
    }
}
