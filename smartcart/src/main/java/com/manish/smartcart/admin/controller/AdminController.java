package com.manish.smartcart.admin.controller;

import com.manish.smartcart.admin.dto.*;
import com.manish.smartcart.admin.service.AdminService;
import com.manish.smartcart.infrastructure.invoice.InvoiceService;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.service.OrderQueryService;
import com.manish.smartcart.payment.service.WebhookDlqService;
import com.manish.smartcart.seller.dto.SellerProductAnalyticsResponse;
import com.manish.smartcart.seller.dto.SellerSummaryResponse;
import com.manish.smartcart.shared.mapper.OrderMapper;
import com.manish.smartcart.user.model.SellerProfile;
import com.manish.smartcart.order.coupon.service.CouponService;
import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.order.dto.ShipmentRequest;
import com.manish.smartcart.order.service.ShipmentService;
import com.manish.smartcart.order.service.ReturnAdminService;
import com.manish.smartcart.shared.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.manish.smartcart.order.coupon.dto.CouponRequest;
import com.manish.smartcart.order.coupon.dto.CouponResponse;
import com.manish.smartcart.notification.service.OrderNotificationService;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Controller", description = "Restricted administrative operations for system management")
@SecurityRequirement(name = "bearerAuth") // Every method in this class requires a JWT
public class AdminController {

    private final AdminService adminService;
    private final CouponService couponService;
    private final OrderNotificationService orderNotificationService;
    private final ShipmentService shipmentService;
    private final WebhookDlqService webhookDlqService;
    private final ReturnAdminService returnAdminService;

    private final OrderQueryService orderQueryService;
    private final InvoiceService invoiceService;
    private final OrderMapper orderMapper;


    @Operation(summary = "Get Dashboard Stats", description = "Retrieves top-selling products and low-stock alerts. Access restricted to users with ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    @ApiResponse(responseCode = "403", description = "Access Denied: Admin role required", content = @Content)
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int pageSize) {
        // threshold: items with stock less than this
        // page/size: pagination for the Top Sellers list
        DashboardResponse adminStats = adminService.getAdminStats(pageNumber, pageSize);
        if (adminStats.getTopSellingProducts().isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "There is no top selling product in the system"));
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(adminStats);
        }
    }

    @Operation(
            summary = "Platform Intelligence Dashboard",
            description = "Retrieves industry-standard BI metrics: Net Revenue, Refund Rates, and the Return Funnel insights."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved intelligence metrics")
    @GetMapping("/analytics/intelligence")
    public ResponseEntity<PlatformIntelligenceResponse> getPlatformIntelligence() {
        return ResponseEntity.ok(adminService.getPlatformIntelligence());
    }

    /**
     * GET /api/v1/admin/analytics/revenue

     * Returns revenue breakdown by product category — sorted highest to lowest.
     * Use case: Admin spots that "Footwear" revenue dropped 40% this week →
     * checks if supplier has a stock issue or a competitor launched a sale.

     * No query params needed: the DB already returns all categories ranked.
     * Frontend can slice/dice the array however it needs (top 5, pie chart, etc.)
     */
    @Operation(
            summary = "Revenue by Category",
            description = "Revenue breakdown grouped by product category. " +
                    "Sorted highest to lowest. Only counts DELIVERED orders — real earned revenue."
    )
    @ApiResponse(responseCode = "200", description = "Category revenue breakdown retrieved")
    @GetMapping("/analytics/category-revenue")
    public ResponseEntity<List<CategoryRevenueDTO>> getCategoryRevenue() {
        return ResponseEntity.ok(adminService.getCategoryRevenueBreakdown());
    }


    /**
     * GET /api/v1/admin/analytics/customers
     * Real-world use cases:
     *  1. Marketing team pulls top=20 → runs VIP loyalty campaign for top spenders
     *  2. Retention team pulls churnAfterDays=45 → sends win-back coupons to at-risk customers
     *  3. Frontend uses riskLevel (HOT/WARM/COLD) to color-code the customer list dashboard
     * Both params have sensible defaults so the endpoint works with zero query params.
     */
    @Operation(
            summary = "Customer Lifetime Value + Churn Risk",
            description = "Returns top customers by lifetime spend AND customers at risk of churning. " +
                    "Use 'top' to control how many VIPs to return. " +
                    "Use 'churnAfterDays' to define what 'inactive' means for your business."
    )
    @ApiResponse(responseCode = "200", description = "Customer intelligence data retrieved")
    @GetMapping("/analytics/customers")
    public ResponseEntity<CustomerIntelligenceResponse>getCustomerIntelligence(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(defaultValue = "60") int churnAfterDays){
        return ResponseEntity.ok(adminService.getCustomerIntelligence(top, churnAfterDays));
    }

    /**
     * GET /api/v1/admin/sellers/{sellerId}/analytics

     * Admin use cases:
     *  1. KYC Review: Before approving a seller, check their product return rates.
     *     A new seller with 40% CRITICAL products is a red flag.
     *  2. Suspension Decisions: If a seller consistently has CRITICAL products,
     *     the admin can suspend their listing rights.
     *  3. Seller Support: When a seller raises a complaint, admin can
     *     pull their quality data to understand the full picture.

     * sellerId comes from the URL path — admin explicitly chooses which
     * seller to inspect. There is zero chance of a data leak between sellers.
     */
    @Operation(
            summary = "View a specific seller's product quality analytics",
            description = "Admin-only view of a seller's product return rates and quality scores. " +
                    "Use during KYC review or seller performance investigations."
    )
    @ApiResponse(responseCode = "200", description = "Seller product analytics retrieved")
    @ApiResponse(responseCode = "404", description = "Seller not found")
    @GetMapping("/sellers/{sellerPublicId}/analytics")
    public ResponseEntity<SellerProductAnalyticsResponse> getSellerProductAnalytics(
            @PathVariable java.util.UUID sellerPublicId) {
        return ResponseEntity.ok(
                adminService.getSellerAnalyticsForAdmin(sellerPublicId)
        );
    }

    @Operation(summary = "Update Order Status", description = "Change the lifecycle state of an order (e.g., PENDING to SHIPPED). Access restricted to Admin.")
    @ApiResponse(responseCode = "200", description = "Order status updated successfully")
    @ApiResponse(responseCode = "404", description = "Order ID not found", content = @Content)
    @PatchMapping("/{orderPublicId}/status")
    public ResponseEntity<?> changeOrderStatus(@PathVariable java.util.UUID orderPublicId,
                                               @RequestBody StatusChangeRequest request) {
        request.setOrderPublicId(orderPublicId);
        var response = adminService.changeTheStatusOfOrders(request);
        orderNotificationService.sendStatusUpdateEmail(response);
        return ResponseEntity.ok(response);
    }

    // --- COUPON MANAGEMENT ---
    @PostMapping("/coupons")
    @Operation(summary = "Create a new platform-wide discount coupon")
    @ApiResponse(responseCode = "200", description = "Coupon created successfully")
    public ResponseEntity<CouponResponse> createCoupon(@Valid
                                                           @RequestBody
                                                           CouponRequest couponRequest) {
        return ResponseEntity.ok(couponService.createCoupon(couponRequest));
    }


    @GetMapping("/coupons")
    @Operation(summary = "View all global coupons")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all coupons")
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }


    @PatchMapping("/coupons/{couponPublicId}/toggle")
    @Operation(summary = "Activate or deactivate a coupon quickly")
    @ApiResponse(responseCode = "200", description = "Coupon status toggled successfully")
    public ResponseEntity<String> toggleCouponStatus(@PathVariable java.util.UUID couponPublicId) {
        couponService.toggleActive(couponPublicId);
        return ResponseEntity.ok("Coupon status toggled successfully.");
    }


    // POST /api/v1/admin/{orderPublicId}/shipment
    // Called once the admin has physically packed and handed the order to the courier
    @PostMapping("/{orderPublicId}/shipment")
    @Operation(summary = "Attach shipment tracking to an order and mark it as SHIPPED")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment attached and Order marked SHIPPED"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> attachShipment(
            @PathVariable java.util.UUID orderPublicId,
            @Valid @RequestBody ShipmentRequest request) {
        return ResponseEntity.ok(shipmentService.attachShipmentAndShip(orderPublicId, request));
    }


    // --- WEBHOOK DLQ MANAGEMENT ---
    @GetMapping("/webhooks/dlq/pending")
    @Operation(summary = "View broken webhooks", description = "Retrieves all failed Razorpay webhooks that need manual admin intervention.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved DLQ items")
    public ResponseEntity<?> getPendingWebhooks() {
        return ResponseEntity.ok(webhookDlqService.getPendingFailures());
    }


    @PostMapping("/webhooks/dlq/{eventPublicId}/replay")
    @Operation(summary = "Re-Play a failed webhook", description = "Simulates Razorpay re-sending the exact JSON payload to your server to fix a dropped payment.")
    @ApiResponse(responseCode = "200", description = "Webhook replayed successfully")
    public ResponseEntity<String> replayFailedWebhook(@PathVariable java.util.UUID eventPublicId) {
        String result = webhookDlqService.replayFailedWebhook(eventPublicId);
        return ResponseEntity.ok(result);
    }

    // --- KYC MANAGEMENT ---
    @GetMapping("/sellers")
    @Operation(summary = "List all sellers", description = "Returns all registered sellers with KYC status.")
    @ApiResponse(responseCode = "200", description = "Sellers retrieved")
    public ResponseEntity<List<SellerSummaryResponse>> getAllSellers() {
        return ResponseEntity.ok(adminService.getAllSellers());
    }

    @GetMapping("/sellers/kyc/pending")
    @Operation(summary = "KYC review queue", description = "Returns sellers in PENDING or IN_REVIEW state.")
    @ApiResponse(responseCode = "200", description = "Pending KYC sellers retrieved")
    public ResponseEntity<List<SellerSummaryResponse>> getPendingKycSellers() {
        return ResponseEntity.ok(adminService.getPendingKycSellers());
    }

    @PatchMapping("/sellers/{sellerPublicId}/kyc")
    @Operation(summary = "Update seller KYC status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KYC updated, email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid transition or missing comment"),
            @ApiResponse(responseCode = "404", description = "Seller not found")
    })
    public ResponseEntity<Map<String,String>> updateSellerKyc(
            @PathVariable java.util.UUID sellerPublicId,
            @Valid @RequestBody KycUpdateRequest request){
        SellerProfile updated = adminService.updateSellerKyc(sellerPublicId, request);

        return ResponseEntity.ok(Map.of(
                "message",   "KYC status updated to " + updated.getKycStatus(),
                "sellerId",  String.valueOf(sellerPublicId),
                "newStatus", updated.getKycStatus().name()));
    }


    @Operation(
            summary = "Approve return request → triggers Razorpay refund",
            description = "Admin approves a RETURN_REQUESTED order. " +
                    "Restores stock, issues Razorpay refund, updates status to REFUNDED, sends refund email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return approved — refund issued"),
            @ApiResponse(responseCode = "400", description = "Order is not in RETURN_REQUESTED state"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderPublicId}/approve-return")
    public ResponseEntity<OrderResponse>approveReturn(@PathVariable java.util.UUID orderPublicId){
        OrderResponse orderResponse = returnAdminService.approveReturn(orderPublicId);
        return ResponseEntity.ok(orderResponse);
    }


    @Operation(
            summary = "Approve replacement / exchange → re-checks stock → marks REPLACEMENT_SHIPPED",
            description = "Admin approves a REPLACEMENT_REQUESTED or EXCHANGE_REQUESTED order. " +
                    "Re-checks live stock at approval time. Deducts stock. " +
                    "Admin then attaches new tracking via POST /admin/{orderId}/shipment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Replacement approved — REPLACEMENT_SHIPPED"),
            @ApiResponse(responseCode = "400", description = "Order not in correct state or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderPublicId}/approve-replacement")
    public ResponseEntity<?> approveReplacement(@PathVariable java.util.UUID orderPublicId) {
        OrderResponse response = returnAdminService.approveReplacement(orderPublicId);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Reject return/replacement request",
            description = "Resets the order status back to DELIVERED, clears active return metadata, and notifies the user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return request rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Order is not in a return-requested state"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderPublicId}/reject-return")
    public ResponseEntity<OrderResponse>rejectReturn(
            @PathVariable java.util.UUID orderPublicId,
            @RequestParam(required = false) String adminComment){
        OrderResponse response = returnAdminService.rejectReturn(orderPublicId, adminComment);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List all pending return / replacement / exchange requests",
            description = "Retrieves all orders in RETURN_REQUESTED, REPLACEMENT_REQUESTED, or EXCHANGE_REQUESTED states."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved pending return requests")
    @GetMapping("/orders/pending-returns")
    public ResponseEntity<List<OrderResponse>> getPendingReturns() {
        List<OrderResponse> responses = returnAdminService.getPendingReturnRequests();
        return ResponseEntity.ok(responses);
    }

    // =====================================================================================
    // INVOICE DOWNLOAD (Admin)
    // =====================================================================================
    /**
     * GET /api/v1/admin/orders/{orderIdentifier}/invoice
     * <p>
     * MOTIVE: Admins need the ability to pull up tax invoices for ANY order in the system
     *         (e.g., for B2B accounting audits or handling support tickets "I lost my invoice").
     * AGENDA: Bypasses the IDOR ownership check since the caller is an Admin.
     */
    @Operation(summary = "Download any order invoice (Admin)", description = "Generates a PDF invoice for any order globally.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF successfully generated"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{orderIdentifier}/invoice")
    public ResponseEntity<byte[]> downloadInvoiceAdmin (@PathVariable String orderIdentifier){

        // 1. Resolve order globally
        Order order = orderQueryService.resolveOrderWithItems(orderIdentifier);

        // 2. Map to DTO (Shipment omitted to save queries, PDF doesn't need it)
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);

        // 3. Generate PDF
        byte[] pdfBytes = invoiceService.generateInvoice(orderResponse);

        // 4. Stream to browser
        String filename = "invoice_" + (order.getOrderNumber()!= null ? order.getOrderNumber() : order.getPublicId()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}