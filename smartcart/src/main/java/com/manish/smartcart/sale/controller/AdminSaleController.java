package com.manish.smartcart.sale.controller;

import com.manish.smartcart.sale.dto.PlatformSaleEventRequest;
import com.manish.smartcart.sale.dto.PlatformSaleEventResponse;
import com.manish.smartcart.shared.enums.ApprovalStatus;
import com.manish.smartcart.sale.service.AdminSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Enterprise Admin endpoints for managing global sale events and performing Quality Control.
 * Protected strictly by the ADMIN role.
 */

@RestController
@RequestMapping("/api/v1/admin/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSaleController {

    private final AdminSaleService adminSaleService;

    @PostMapping("/events")
    public ResponseEntity<PlatformSaleEventResponse> createEvent(
            @Valid @RequestBody PlatformSaleEventRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminSaleService.createEvent(request));
    }

    @GetMapping("/events")
    public ResponseEntity<List<PlatformSaleEventResponse>> getAllEvents() {
        return ResponseEntity.ok(adminSaleService.getAllEvents());
    }

    @PatchMapping("/items/{itemPublicId}/review")
    public ResponseEntity<String> reviewSellerSubmission(
            @PathVariable UUID itemPublicId,
            @RequestParam ApprovalStatus status) {
        adminSaleService.reviewSellerSubmission (itemPublicId, status);
        return ResponseEntity.ok("Successfully marked submission as " + status.name());
    }
}
