package com.manish.smartcart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.dto.order.ReturnRequestDTO;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.order.OrderQueryService;
import com.manish.smartcart.service.order.OrderReturnService;
import com.manish.smartcart.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, order history, cancellation, and post-delivery requests")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/orders")
public class OrderController {

        private final OrderService orderService;
        private final ObjectMapper objectMapper;
        private final OrderQueryService orderQueryService;
        private final OrderReturnService orderReturnService;

        @Operation(summary = "Checkout and place order", description =  "Processes the cart and creates a permanent order record. "
                + "Snapshots address, pricing, and return policy. "
                + "Confirmation email sent after payment verification.")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Order placed — Razorpay order ID returned"),
                @ApiResponse(responseCode = "400", description = "Cart empty or validation failed"),
                @ApiResponse(responseCode = "409", description = "Insufficient stock (race condition)")
        })
        @PostMapping("/checkout")
        public ResponseEntity<?> placeOrder(
                @Valid @RequestBody OrderRequest orderRequest,
                Authentication authentication) {
                Long userId = extractUserId(authentication);
                OrderResponse orderResponse = orderService.placeOrder(userId, orderRequest);
                return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        }


        @Operation(summary = "Order history",
                description = "Paginated past orders for the authenticated customer. "
                        + "Default: page=0, size=10, sorted by orderDate DESC.")
        @ApiResponse(responseCode = "200", description = "Paginated order history")
        @GetMapping("/history")
        public ResponseEntity<?> getOrderHistory(
                Authentication authentication,
                @PageableDefault(
                        size = 10,
                        sort = "orderDate",
                        direction = Sort.Direction.DESC) Pageable pageable) {
                Long userId = extractUserId(authentication);
                Page<OrderResponse> history = orderQueryService.getOrderHistoryForUser(userId, pageable);
                return ResponseEntity.ok(history);
        }


        @Operation(summary = "Cancel order",
                description = "Cancels an order if it has not been shipped. "
                        + "Restores stock. Issues Razorpay refund if already paid.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Order cancelled"),
                @ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state"),
                @ApiResponse(responseCode = "404", description = "Order not found")
        })
        @PutMapping("/{orderId}/cancel")
        public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                             Authentication authentication){
                Long userId = extractUserId(authentication);
                OrderResponse orderResponse = orderService.cancelOrder(userId, orderId);
                return ResponseEntity.ok(orderResponse);
        }


        @Operation(summary = "Request return / replacement / exchange",
                description = "Submit a post-delivery request. "
                        + "DEFECTIVE, WRONG_ITEM, DAMAGED_IN_TRANSIT require image proof. "
                        + "Send as multipart/form-data with 'request' (JSON) and optional 'images' parts.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Request submitted — order status updated"),
                @ApiResponse(responseCode = "400", description = "Window expired / type not allowed / duplicate request"),
                @ApiResponse(responseCode = "403", description = "Order does not belong to you"),
                @ApiResponse(responseCode = "404", description = "Order not found")
        })
        @PostMapping(value = "/{orderId}/request-return", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        public ResponseEntity<OrderResponse>requestReturn(
                @PathVariable Long orderId,
                @RequestPart("request") String requestJson,
                @RequestPart(value = "images", required = false) MultipartFile[] images,
                Authentication authentication) throws Exception {

                ReturnRequestDTO request = objectMapper.readValue(requestJson, ReturnRequestDTO.class);

                if (request.getReturnType() == null) {
                        throw new BusinessLogicException("returnType is required: RETURN, REPLACEMENT, or EXCHANGE");
                }
                if (request.getReturnReason() == null) {
                        throw new BusinessLogicException(
                                "returnReason is required. Valid: DEFECTIVE, WRONG_ITEM, "
                                        + "DAMAGED_IN_TRANSIT, CHANGED_MIND, NOT_AS_DESCRIBED, SIZE_MISMATCH");
                }

                Long userId = extractUserId(authentication);
                OrderResponse orderResponse = orderReturnService.requestReturn(
                        userId, orderId,
                        request.getReturnType(),
                        request.getReturnReason(),
                        request.getReturnDescription(),
                        images
                );

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
