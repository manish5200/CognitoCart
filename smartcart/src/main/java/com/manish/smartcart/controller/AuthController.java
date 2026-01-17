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

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Endpoints for user registration and login")
public class AuthController {

     private final AuthService authService;

     //Register
     @Operation(summary = "Register new user")
     @ApiResponse(responseCode = "201", description = "User registered successfully")
     @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
     @PostMapping("/register/customer")
     public ResponseEntity<Map<String,String>>registerCustomer(@Valid @RequestBody CustomerAuthRequest authRequest){
         authService.registerCustomer(authRequest);
         return ResponseEntity.status(HttpStatus.CREATED)
                 .body(Map.of("message", "Customer registered successfully. ✅"));
     }

    @Operation(summary = "Register new seller")
    @ApiResponse(responseCode = "201", description = "Seller registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or seller already exists")
    @PostMapping("/register/seller")
    public ResponseEntity<?>registerSeller(@Valid @RequestBody SellerAuthRequest authRequest){
        authService.registerSeller(authRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Seller registered successfully. ✅"));
    }

    // --- AUTHENTICATION ---

    //Login
    @Operation(summary = "User/Seller Login", description = "Returns Access (stateless) and Refresh (stateful) tokens.")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Refresh Access Token", description = "Implements Refresh Token Rotation for security.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "403", description = "Refresh token expired or invalid")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }



}
