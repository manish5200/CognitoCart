package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.seller.SellerDashboardResponse;
import com.manish.smartcart.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Seller profile and dashboard endpoints")
public class SellerController {

    private final SellerService sellerService;

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
    public ResponseEntity<SellerDashboardResponse> getDashboard(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long sellerId = userDetails.getUser().getId();
        return ResponseEntity.ok(sellerService.getDashboard(sellerId));
    }
}
