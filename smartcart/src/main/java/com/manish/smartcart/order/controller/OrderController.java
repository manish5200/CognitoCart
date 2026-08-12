package com.manish.smartcart.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.order.dto.OrderEventResponse;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.service.OrderEventService;
import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.order.dto.OrderRequest;
import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.order.dto.ReturnRequestDTO;
import com.manish.smartcart.shared.enums.RefundDestination;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.order.service.OrderQueryService;
import com.manish.smartcart.order.service.OrderReturnService;
import com.manish.smartcart.order.service.OrderService;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, order history, cancellation, and post-delivery requests")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/orders")
public class OrderController {

        private final OrderService orderService;
        private final ObjectMapper objectMapper;
        private final OrderEventService orderEventService;
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
                description = "Cancels an order if it has not been shipped. Accepts UUID or Human Order Number.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Order cancelled"),
                @ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state"),
                @ApiResponse(responseCode = "404", description = "Order not found")
        })
        @PutMapping("/{orderIdentifier}/cancel")
        public ResponseEntity<?> cancelOrder(
                @PathVariable String orderIdentifier,
                @RequestParam(defaultValue = "ORIGINAL") RefundDestination refundDestination,
                        Authentication authentication){
                Long userId = extractUserId(authentication);

                // 1. Resolve the external identifier to our internal database Entity
                Order order = orderQueryService.resolveOrder(orderIdentifier);

                // 2. Pass the internal Long ID to the core service (Keeping DB logic isolated)
                OrderResponse orderResponse = orderService.cancelOrder(userId, order.getId(), refundDestination);
                return ResponseEntity.ok(orderResponse);
        }


        @Operation(summary = "Request return / replacement / exchange",
                description = "Submit a post-delivery request. Accepts UUID or Human Order Number.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Request submitted — order status updated"),
                @ApiResponse(responseCode = "400", description = "Window expired / type not allowed / duplicate request"),
                @ApiResponse(responseCode = "403", description = "Order does not belong to you"),
                @ApiResponse(responseCode = "404", description = "Order not found")
        })
        @PostMapping(value = "/{orderIdentifier}/request-return", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        public ResponseEntity<OrderResponse>requestReturn(
                @PathVariable String orderIdentifier    ,
                @RequestPart("request") String requestJson,
                @RequestPart(value = "images", required = false) MultipartFile[] images,
                Authentication authentication) throws Exception {

                ReturnRequestDTO request = objectMapper.readValue(requestJson, ReturnRequestDTO.class);

                // Validation logic remains the same...
                if (request.getReturnType() == null) {
                        throw new BusinessLogicException("returnType is required: RETURN, REPLACEMENT, or EXCHANGE");
                }
                if (request.getReturnReason() == null) {
                        throw new BusinessLogicException(
                                "returnReason is required. Valid: DEFECTIVE, WRONG_ITEM, "
                                        + "DAMAGED_IN_TRANSIT, CHANGED_MIND, NOT_AS_DESCRIBED, SIZE_MISMATCH");
                }

                Long userId = extractUserId(authentication);

                // 1. Resolve the external identifier to our internal database Entity
                Order order = orderQueryService.resolveOrder(orderIdentifier);

                // 2. Pass the internal Long ID to the core service
                OrderResponse orderResponse = orderReturnService.requestReturn(
                        userId,
                        order.getId(),
                        request.getReturnType(),
                        request.getReturnReason(),
                        request.getReturnDescription(),
                        images
                );
                return ResponseEntity.ok(orderResponse);
        }

        @Operation(
                summary = "Get order status timeline",
                description = "Returns the complete chronological audit trail of every status " +
                        "transition for an order. Accepts UUID or Human Order Number (ORD-...)."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Timeline returned successfully"),
                @ApiResponse(responseCode = "404", description = "Order not found or not yours")
        })
        @GetMapping("/{orderIdentifier}/timeline")
        public ResponseEntity<List<OrderEventResponse>> getOrderTimeline(
                @PathVariable String orderIdentifier,
                Authentication authentication){
                Long userId = extractUserId(authentication);
                // Smart resolver: handles both UUID and ORD-YYYYMMDD-XXXXXX human ID
                Order order = orderQueryService.resolveOrder(orderIdentifier);

                // IDOR Guard: return 404 (not 403) to prevent order ID enumeration
                if(!order.getUser().getId().equals(userId)){
                        throw new ResourceNotFoundException("Order not found: " + orderIdentifier);
                }

                return ResponseEntity.ok(orderEventService.getTimeline(order.getId()));
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
