package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@Tag(name = "Order Processing", description = "Checkout, history, and order cancellation")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/orders")
public class OrderController {

        private final OrderService orderService;

        @Operation(summary = "Checkout and place order", description = "Processes the cart and creates a permanent order record. "
                        +
                        "Snapshots address and pricing. Email will be sent Automatically")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Cart is empty or validation failed"),
            @ApiResponse(responseCode = "409", description = "Stock race condition failed (Insufficient stock)")
        })
        @PostMapping("/checkout")
        public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest orderRequest,
                        Authentication authentication) {
                Long userId = extractUserId(authentication);
                OrderResponse orderResponse = orderService.placeOrder(userId, orderRequest);
                return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        }


        @Operation(summary = "Get order history", description = "Retrieves paginated past orders for the authenticated user.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated order history retrieved")
        })
        @GetMapping("/history")
        public ResponseEntity<?> getOrderHistory(
                Authentication authentication,
                @PageableDefault(
                        size = 10,
                        sort = "orderDate",
                        direction = Sort.Direction.DESC) Pageable pageable) {
                Long userId = extractUserId(authentication);
                Page<OrderResponse> history = orderService.getOrderHistoryForUser(userId, pageable);
                return ResponseEntity.ok(history);
        }


        @Operation(summary = "Cancel order", description = "Allows a user to cancel an order if it has not yet been processed for shipping.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order has already shipped or cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found")
        })
        @PutMapping("/{orderId}/cancel")
        public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                             Authentication authentication){
                Long userId = extractUserId(authentication);
                OrderResponse orderResponse = orderService.cancelOrder(userId, orderId);
                return ResponseEntity.ok(orderResponse);
        }

        //HELPER FUNCTION TO EXTRACT UserId
        private long extractUserId(Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                if(customUserDetails == null){
                        throw new BusinessLogicException("Authentication context is missing. Please log in again.");
                }
                return customUserDetails.getUser().getId();
        }
}
