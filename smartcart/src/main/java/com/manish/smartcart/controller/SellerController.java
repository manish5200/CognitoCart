package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.seller.SellerDashboardResponse;
import com.manish.smartcart.service.SellerAnalyticsExportService;
import com.manish.smartcart.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Seller profile and dashboard endpoints")
public class SellerController {

    private final SellerService sellerService;
    private final SellerAnalyticsExportService  sellerAnalyticsExportService;

    /**
     * GET /api/v1/sellers/dashboard
     * Returns a complete snapshot of the authenticated seller's business:
     * - Product counts (total / available / out-of-stock)
     * - Revenue (delivered vs pending)
     * - Order stats by status
     * - Top 5 products by units sold
     * Access: SELLER only (each seller sees ONLY their own data)
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Seller dashboard", description = "Returns product stats, revenue, order stats and top products for the authenticated seller")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved seller dashboard")
    public ResponseEntity<SellerDashboardResponse> getDashboard(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long sellerId = userDetails.getUser().getId();
        return ResponseEntity.ok(sellerService.getDashboard(sellerId));
    }


    //SELLER REVENUE GENERATION
    @GetMapping(value = "/reports/revenue.csv", produces = "text/csv")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Export Revenue CSV", description = "Streams an ultra-fast CSV file containing all delivered orders without locking the server.")
    @ApiResponse(responseCode = "200", description = "CSV Stream initialized successfully")
    public ResponseEntity<StreamingResponseBody> downloadRevenueCsv(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long sellerId = userDetails.getUser().getId();
        StreamingResponseBody stream = sellerAnalyticsExportService.exportOrdersToCsvStream(sellerId);

        // Generate timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileName = "seller_revenue_report_" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }
}
