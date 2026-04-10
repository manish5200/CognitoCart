package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.customer.CustomerDashboardDTO;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.CustomerService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Operations", description = "Customer-specific personalized data and dashboards")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;


    @Operation(
            summary = "Get customer dashboard",
            description = "Returns personalized stats: total orders, recent activity, and spend summary. " +
                    "Data is scoped strictly to the logged-in customer."
    )
    @ApiResponse(responseCode = "200", description = "Customer dashboard retrieved successfully")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getCustomerDashboard(Authentication authentication,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) Integer pageSize) {
        Long userId = extractUserId(authentication);
        CustomerDashboardDTO customerStats = customerService.getCustomerDashboard(userId, pageNumber, pageSize);
        return ResponseEntity.ok(customerStats);

    }

    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new BusinessLogicException("Authentication context is missing. Please log in again.");
        }
        return userDetails.getUser().getId();
    }
}
