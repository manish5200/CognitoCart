package com.manish.smartcart.controller;

import com.manish.smartcart.dto.admin.DashboardResponse;
import com.manish.smartcart.dto.admin.StatusChangeRequest;
import com.manish.smartcart.dto.order.ShipmentRequest;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.service.AdminService;
import com.manish.smartcart.service.ShipmentService;
import com.manish.smartcart.service.WebhookDlqService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.manish.smartcart.dto.coupon.CouponRequest;
import com.manish.smartcart.dto.coupon.CouponResponse;
import com.manish.smartcart.service.CouponService;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "2. Admin Controller", description = "Restricted administrative operations for system management")
@SecurityRequirement(name = "bearerAuth") // Every method in this class requires a JWT
public class AdminController {

    private final AdminService adminService;
    private final OrderMapper orderMapper;
    private final CouponService couponService;
    private final OrderNotificationService orderNotificationService;
    private final ShipmentService shipmentService;
    private final WebhookDlqService webhookDlqService;

    @Operation(summary = "Get Dashboard Stats", description = "Retrieves top-selling products and low-stock alerts. Access restricted to users with ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    @ApiResponse(responseCode = "403", description = "Access Denied: Admin role required", content = @Content)
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam(name = "stockThreshold", defaultValue = AppConstants.LOW_STOCK_THRESHOLD
                    + "") int stockThreshold,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int pageSize) {
        // threshold: items with stock less than this
        // page/size: pagination for the Top Sellers list
        DashboardResponse adminStats = adminService.getAdminStats(stockThreshold, pageNumber, pageSize);
        if (adminStats.getTopSellingProducts().isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "There is no top selling product in the system"));
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(adminStats);
        }
    }

    @Operation(summary = "Update Order Status", description = "Change the lifecycle state of an order (e.g., PENDING to SHIPPED). Access restricted to Admin.")
    @ApiResponse(responseCode = "200", description = "Order status updated successfully")
    @ApiResponse(responseCode = "404", description = "Order ID not found", content = @Content)
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> changeOrderStatus(@PathVariable Long orderId,
                                               @RequestBody StatusChangeRequest request) {
        request.setOrderId(orderId);
        Order order = adminService.changeTheStatusOfOrders(request);

        // Map to response and send notification
        var response = orderMapper.toOrderResponse(order);
        orderNotificationService.sendStatusUpdateEmail(response);

        return ResponseEntity.ok(response);
    }

    // --- COUPON MANAGEMENT ---

    @PostMapping("/coupons")
    @Operation(summary = "Create a new platform-wide discount coupon")
    public ResponseEntity<CouponResponse> createCoupon(@Valid
                                                           @RequestBody
                                                           CouponRequest couponRequest) {
        return ResponseEntity.ok(couponService.createCoupon(couponRequest));
    }

    @GetMapping("/coupons")
    @Operation(summary = "View all global coupons")
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PatchMapping("/coupons/{couponId}/toggle")
    @Operation(summary = "Activate or deactivate a coupon quickly")
    public ResponseEntity<String> toggleCouponStatus(@PathVariable Long couponId) {
        couponService.toggleActive(couponId);
        return ResponseEntity.ok("Coupon status toggled successfully.");
    }

    // POST /api/v1/admin/{orderId}/shipment
    // Called once the admin has physically packed and handed the order to the courier
    @PostMapping("/{orderId}/shipment")
    @Operation(summary = "Attach shipment tracking to an order and mark it as SHIPPED")
    public ResponseEntity<?> attachShipment(
            @PathVariable Long orderId,
            @Valid @RequestBody ShipmentRequest request) {
        return ResponseEntity.ok(shipmentService.attachShipmentAndShip(orderId, request));
    }

    // --- WEBHOOK DLQ MANAGEMENT ---
    @GetMapping("/webhooks/dlq/pending")
    @Operation(summary = "View broken webhooks", description = "Retrieves all failed Razorpay webhooks that need manual admin intervention.")
    public ResponseEntity<?> getPendingWebhooks() {
        return ResponseEntity.ok(webhookDlqService.getPendingFailures());
    }
    @PostMapping("/webhooks/dlq/{eventId}/replay")
    @Operation(summary = "Re-Play a failed webhook", description = "Simulates Razorpay re-sending the exact JSON payload to your server to fix a dropped payment.")
    public ResponseEntity<String> replayFailedWebhook(@PathVariable Long eventId) {
        String result = webhookDlqService.replayFailedWebhook(eventId);
        return ResponseEntity.ok(result);
    }
}