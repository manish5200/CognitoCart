package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.ReturnPolicyRequest;
import com.manish.smartcart.dto.product.ReturnPolicyResponse;
import com.manish.smartcart.dto.seller.SellerDashboardResponse;
import com.manish.smartcart.service.ReturnPolicyService;
import com.manish.smartcart.service.SellerAnalyticsExportService;
import com.manish.smartcart.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Seller profile and dashboard endpoints")
public class SellerController {

    private final SellerService sellerService;
    private final SellerAnalyticsExportService  sellerAnalyticsExportService;
    private final ReturnPolicyService returnPolicyService;

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
        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(sellerService.getDashboard(sellerId));
    }


    //SELLER REVENUE GENERATION
    @GetMapping(value = "/reports/revenue.csv", produces = "text/csv")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Export Revenue CSV", description = "Streams an ultra-fast CSV file containing all delivered orders without locking the server.")
    @ApiResponse(responseCode = "200", description = "CSV Stream initialized successfully")
    public ResponseEntity<StreamingResponseBody> downloadRevenueCsv(Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
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

    //---------------- CRUD OPERATION RELATED TO RETURN POLICY -----------------------------
    @Operation(summary = "Create return policy",
            description = "Set return/exchange policy for your product OR a category. "
                    + "productId XOR categoryId. returnWindowDays max=30. "
                    + "NON_RETURNABLE must have all flags=false and days=0.")
    @ApiResponse(responseCode = "201", description = "Policy created")
    @ApiResponse(responseCode = "400", description = "Validation failed or duplicate policy exists")
    @ApiResponse(responseCode = "403", description = "Product does not belong to you")
    @PostMapping("/return-policy")
    @PreAuthorize("hasRole('SELLER')")
    public ReturnPolicyResponse createPolicy(
            @Valid @RequestBody ReturnPolicyRequest request,
            Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
        ReturnPolicyResponse savedPolicy = returnPolicyService.createPolicy(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPolicy).getBody();
    }

    @Operation(summary = "List my return policies",
            description = "Returns all return policies configured for your products.")
    @ApiResponse(responseCode = "200", description = "Policies retrieved")
    @GetMapping("/return-policy")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ReturnPolicyResponse>> getMyPolicies(Authentication authentication) {
        return ResponseEntity.ok(returnPolicyService.getMyPolicies(extractSellerId(authentication)));
    }


    @Operation(summary = "Update return policy",
            description = "Update an existing policy. Only works on your own product policies.")
    @ApiResponse(responseCode = "200", description = "Policy updated")
    @ApiResponse(responseCode = "404", description = "Policy not found or not yours")
    @PutMapping("/return-policy/{policyId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ReturnPolicyResponse> updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody ReturnPolicyRequest request,
            Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(returnPolicyService.updatePolicy(sellerId, policyId, request));
    }

    @Operation(summary = "Delete return policy",
            description = "Removes a policy. Product falls back to category or NON_RETURNABLE default.")
    @ApiResponse(responseCode = "200", description = "Policy deleted")
    @DeleteMapping("/return-policy/{policyId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deletePolicy(
            @PathVariable Long policyId,
            Authentication authentication) {
        returnPolicyService.deletePolicy(extractSellerId(authentication), policyId);
        return ResponseEntity.ok(Map.of(
                "message", "Policy deleted. Product now falls back to category or NON_RETURNABLE default."));
    }

    private Long extractSellerId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}
