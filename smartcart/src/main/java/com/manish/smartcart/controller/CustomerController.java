package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.customer.CustomerDashboardDTO;
import com.manish.smartcart.service.CustomerService;
import com.manish.smartcart.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "3. Customer Operations", description = "Customer-specific personalized data and dashboards")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/customer")
public class CustomerController {

    private final CustomerService customerService;
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Get customer dashboard",
            description = "Retrieves personalized stats, recent orders, and recommendations for the logged-in customer.")
    @GetMapping("/dashboard")
    public ResponseEntity<?>getCustomerDashboard(Authentication authentication,
                                                 @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) Integer pageNumber,
                                                 @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) Integer PageSize) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assert userDetails != null;
        Long userId = userDetails.getUserId();
            CustomerDashboardDTO customerStats = customerService.getCustomerDashboard(userId, pageNumber, PageSize);
            return new ResponseEntity<>(customerStats, HttpStatus.OK);

    }
}
