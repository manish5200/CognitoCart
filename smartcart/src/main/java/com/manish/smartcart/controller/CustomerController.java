package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.customer.CustomerDashboardDTO;
import com.manish.smartcart.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@RequestMapping("api/customer")
public class CustomerController {

    private final CustomerService customerService;
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?>getCustomerDashboard(Authentication authentication,
                                                 @RequestParam(defaultValue = "0") Integer pageNumber,
                                                 @RequestParam(defaultValue = "5") Integer PageSize) {

        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            CustomerDashboardDTO customerStats = customerService.getCustomerDashboard(userId, pageNumber, PageSize);
            return new ResponseEntity<>(customerStats, HttpStatus.OK);
        }catch (Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }
}
