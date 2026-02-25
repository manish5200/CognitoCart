package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "6. Order Processing", description = "Checkout, history, and order cancellation")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/orders")
public class OrderController {

        private final OrderService orderService;

        public OrderController(OrderService orderService) {
                this.orderService = orderService;
        }

        @Operation(summary = "Checkout and place order", description = "Processes the cart and creates a permanent order record. "
                        +
                        "Snapshots address and pricing. Email will be sent Automatically")
        @ApiResponse(responseCode = "200", description = "Order placed successfully")
        @PostMapping("/checkout")
        public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest orderRequest,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();
                OrderResponse orderResponse = orderService.placeOrder(userId, orderRequest);
                return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        }

        @Operation(summary = "Get order history", description = "Retrieves all past orders for the authenticated user.")
        @GetMapping("/history")
        public ResponseEntity<?> getOrderHistory(Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();

                List<OrderResponse> history = orderService.getOrderHistoryForUser(userId);
                return ResponseEntity.ok(history);
        }

        @Operation(summary = "Cancel order", description = "Allows a user to cancel an order if it has not yet been processed for shipping.")
        @PutMapping("/{orderId}/cancel")
        public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                Long userId = userDetails.getUser().getId();
                OrderResponse orderResponse = orderService.cancelOrder(userId, orderId);
                return ResponseEntity.ok(orderResponse);
        }
}
