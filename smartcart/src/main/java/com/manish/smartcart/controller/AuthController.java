package com.manish.smartcart.controller;

import com.manish.smartcart.dto.auth.*;
import com.manish.smartcart.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    // --- REGISTRATION ---

    @Operation(summary = "Register new customer", description = "Creates a customer account. Name, email, password (min 8 chars), and phone are required. DOB and gender are optional.")
    @ApiResponse(responseCode = "201", description = "Customer registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed or duplicate email/phone")
    @PostMapping("/register/customer")
    public ResponseEntity<RegisterResponse> registerCustomer(@Valid @RequestBody CustomerAuthRequest authRequest) {
        RegisterResponse response = authService.registerCustomer(authRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Register new seller", description = "Creates a seller account. Owner name, store name, email, password (min 8 chars), phone, and business address are required. GSTIN/PAN are optional at signup. KYC starts as PENDING.")
    @ApiResponse(responseCode = "201", description = "Seller registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed or duplicate email/phone")
    @PostMapping("/register/seller")
    public ResponseEntity<RegisterResponse> registerSeller(@Valid @RequestBody SellerAuthRequest authRequest) {
        RegisterResponse response = authService.registerSeller(authRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- AUTHENTICATION ---

    @Operation(summary = "Login", description = "Returns a stateless access token (15 min) and a stateful refresh token (1 hr).")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Refresh access token", description = "Implements Refresh Token Rotation — the old refresh token is invalidated and a new pair is issued.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "403", description = "Refresh token expired or invalid")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
