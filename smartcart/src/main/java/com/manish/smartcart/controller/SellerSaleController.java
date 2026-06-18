package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.FlashSaleItemRequest;
import com.manish.smartcart.dto.product.FlashSaleItemResponse;
import com.manish.smartcart.service.SellerSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    private Long extractUserId(Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}
