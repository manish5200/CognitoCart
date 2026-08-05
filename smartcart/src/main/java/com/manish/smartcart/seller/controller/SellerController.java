package com.manish.smartcart.seller.controller;

import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.product.dto.ReturnPolicyRequest;
import com.manish.smartcart.product.dto.ReturnPolicyResponse;
import com.manish.smartcart.seller.dto.SellerDashboardResponse;
import com.manish.smartcart.seller.dto.SellerProductAnalyticsResponse;
import com.manish.smartcart.infrastructure.returnpolicy.ReturnPolicyService;
import com.manish.smartcart.seller.service.SellerAnalyticsExportService;
import com.manish.smartcart.seller.service.SellerService;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

/**
 * Pure Edge API Gateway for Seller Operations.
 * <p>
 * ARCHITECTURAL CONTEXT:
 * This controller acts purely as an HTTP ingress point. It is responsible for:
 * 1. Protocol Translation (HTTP -> Java Objects).
 * 2. Hardened Security Assertion (Extracting & validating JWT claims).
 * 3. Delegating business logic to the core domain services.
 * <p>
 * It intentionally contains zero business logic, ensuring the domain services
 * remain agnostic of the delivery mechanism (HTTP/REST).
 */
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Seller profile and dashboard endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {

    private final SellerService sellerService;
    private final SellerAnalyticsExportService sellerAnalyticsExportService;
    private final ReturnPolicyService returnPolicyService;

    /**
     * Dashboard Data Aggregation endpoint.
     * <p>
     * PERFORMANCE IMPLICATION:
     * Currently delegates to real-time transactional queries. If seller concurrency
     * scales > 1,000 active sessions/min, this should be migrated to read from a
     * Redis-cached Materialized View to protect the primary RDS instance from CPU spikes.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Seller dashboard", description = "Returns product stats, revenue, order stats and top products for the authenticated seller")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved seller dashboard")
    public ResponseEntity<SellerDashboardResponse> getDashboard(Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(sellerService.getDashboard(sellerId));
    }

    /**
     * High-Volume Data Egress (CSV Export).
     * <p>
     * MEMORY MANAGEMENT DESIGN:
     * We return a StreamingResponseBody instead of a byte[] or String.
     * Returning a byte[] for a seller with 50,000 orders would pull ~25MB of data
     * into the JVM Heap all at once. Doing this concurrently would trigger an OutOfMemoryError.
     * <p>
     * By yielding a Stream, Spring writes the data directly to the HTTP Socket buffer
     * in chunks (via the Tomcat thread pool), bypassing the JVM heap almost entirely.
     */
    @GetMapping(value = "/reports/revenue.csv", produces = "text/csv")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Export Revenue CSV", description = "Streams an ultra-fast CSV file containing all delivered orders without locking the server.")
    @ApiResponse(responseCode = "200", description = "CSV Stream initialized successfully")
    public ResponseEntity<StreamingResponseBody> downloadRevenueCsv(Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
        StreamingResponseBody stream = sellerAnalyticsExportService.exportOrdersToCsvStream(sellerId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "seller_revenue_report_" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    // ---------------- CRUD OPERATIONS: RETURN POLICY -----------------------------

    /**
     * Creates a new return policy configuration.
     * <p>
     * IDEMPOTENCY/STATE:
     * POST is inherently non-idempotent. The underlying returnPolicyService must
     * implement a UPSERT pattern or unique constraints to prevent duplicating policies
     * if the client retries a timed-out network request.
     */
    @PostMapping("/return-policy")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create return policy", description = "Set return/exchange policy for your product OR a category.")
    @ApiResponse(responseCode = "201", description = "Policy created")
    public ResponseEntity<ReturnPolicyResponse> createPolicy(
            @Valid @RequestBody ReturnPolicyRequest request,
            Authentication authentication) {

        Long sellerId = extractSellerId(authentication);
        ReturnPolicyResponse savedPolicy = returnPolicyService.createPolicy(sellerId, request);

        // Strict adherence to HTTP semantics: 201 Created for resource generation
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPolicy);
    }

    @GetMapping("/return-policy")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "List my return policies", description = "Returns all return policies configured for your products.")
    @ApiResponse(responseCode = "200", description = "Policies retrieved")
    public ResponseEntity<List<ReturnPolicyResponse>> getMyPolicies(Authentication authentication) {
        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(returnPolicyService.getMyPolicies(sellerId));
    }

    /**
     * Mutates an existing policy.
     * <p>
     * SECURITY BOUNDARY:
     * The UUID (policyPublicId) prevents enumeration attacks. Furthermore, the Service layer
     * MUST validate that the resolved policy actually belongs to the caller's sellerId to
     * prevent Insecure Direct Object Reference (IDOR).
     */
    @PutMapping("/return-policy/{policyPublicId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update return policy", description = "Update an existing policy. Only works on your own product policies.")
    public ResponseEntity<ReturnPolicyResponse> updatePolicy(
            @PathVariable UUID policyPublicId,
            @Valid @RequestBody ReturnPolicyRequest request,
            Authentication authentication) {

        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(returnPolicyService.updatePolicy(sellerId, policyPublicId, request));
    }

    @DeleteMapping("/return-policy/{policyPublicId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete return policy")
    public ResponseEntity<?> deletePolicy(
            @PathVariable UUID policyPublicId,
            Authentication authentication) {

        Long sellerId = extractSellerId(authentication);
        returnPolicyService.deletePolicy(sellerId, policyPublicId);

        return ResponseEntity.ok(Map.of("message", "Policy deleted. Product now falls back to category or NON_RETURNABLE default."));
    }

    // ---------------- ANALYTICS -----------------------------

    /**
     * Intelligent Product Analytics Feed.
     * <p>
     * DOMAIN CONTEXT:
     * Computes real-time return velocity. Used to proactively identify defective
     * SKUs before they degrade the platform's overall customer trust score.
     */
    @GetMapping("/analytics/products")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Product Quality Score Dashboard")
    public ResponseEntity<SellerProductAnalyticsResponse> getProductQualityAnalytics(
            Authentication authentication) {

        Long sellerId = extractSellerId(authentication);
        return ResponseEntity.ok(sellerService.getProductQualityAnalytics(sellerId));
    }

    // ---------------- INTERNAL HELPERS -----------------------------
    private Long extractSellerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessLogicException("Authentication context is missing or invalid. Please log in again.");
        }
        return userDetails.getUser().getId();
    }
}