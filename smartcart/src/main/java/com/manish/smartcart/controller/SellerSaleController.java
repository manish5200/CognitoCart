package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.FlashSaleItemRequest;
import com.manish.smartcart.dto.product.FlashSaleItemResponse;
import com.manish.smartcart.service.SellerBulkSaleService;
import com.manish.smartcart.service.SellerSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Enterprise Seller endpoints for opting-in variants to Admin-created events.
 * Protected strictly by the SELLER role.
 */
@RestController
@RequestMapping("/api/v1/seller/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerSaleController {

    private final SellerSaleService sellerSaleService;
    private final SellerBulkSaleService sellerBulkSaleService;

    @PostMapping("/items")
    public ResponseEntity<FlashSaleItemResponse> submitFlashSaleItem(
            @Valid @RequestBody FlashSaleItemRequest request,
            Authentication authentication) { // Safely extracts logged-in Seller ID
        Long sellerId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerSaleService.submitFlashSaleItem(request,sellerId));
    }

    @GetMapping("/items")
    public ResponseEntity<List<FlashSaleItemResponse>> getMySubmissions(Authentication authentication) {
        Long sellerId = extractUserId(authentication);
        return ResponseEntity.ok(sellerSaleService.getSellerSubmissions(sellerId));
    }

    /**
     * Bulk CSV Upload — Seller opts-in up to 500 SKUs in one HTTP request.

     * HTTP Content-Type: multipart/form-data
     * Form field name:   "file"

     * CSV must have header: variant_id, discount_percentage, max_units, max_units_per_user
     * Returns a plain-text summary: "✅ 498 submitted. ❌ 2 skipped"
     */
    @PostMapping("/{eventId}/bulk-upload")
    public ResponseEntity<String>uploadBulkCsv(
        @PathVariable Long eventId,
        @RequestParam("file") MultipartFile file,
        Authentication authentication){
        Long sellerId = extractUserId(authentication);
        String result = sellerBulkSaleService.processBulkCsv(file, eventId, sellerId);
        return ResponseEntity.ok(result);
    }

    private Long extractUserId(Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}
