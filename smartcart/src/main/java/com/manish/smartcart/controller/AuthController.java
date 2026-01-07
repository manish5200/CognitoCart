package com.manish.smartcart.controller;

import com.manish.smartcart.dto.auth.CustomerAuthRequest;
import com.manish.smartcart.dto.auth.LoginRequest;
import com.manish.smartcart.dto.auth.SellerAuthRequest;
import com.manish.smartcart.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Endpoints for user registration and login")
public class AuthController {

     private final AuthService authService;

     public AuthController(AuthService authService) {
         this.authService = authService;
     }

     //Register
     @Operation(summary = "Register new user", description = "Creates a new user account and returns success message.")
     @ApiResponse(responseCode = "201", description = "User registered successfully")
     @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
     @PostMapping("/register/customer")
     public ResponseEntity<?>registerCustomer(@RequestBody CustomerAuthRequest authRequest){
        return authService.registerCustomer(authRequest);
     }

    @Operation(summary = "Register new seller", description = "Creates a new seller account and returns success message.")
    @ApiResponse(responseCode = "201", description = "Seller registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or seller already exists")
    @PostMapping("/register/seller")
    public ResponseEntity<?>registerSeller(@RequestBody SellerAuthRequest authRequest){
        return authService.registerSeller(authRequest);
    }

    //Login
    @Operation(summary = "User/Seller Login", description = "Authenticates user credentials and returns a JWT Bearer Token.")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }



}
