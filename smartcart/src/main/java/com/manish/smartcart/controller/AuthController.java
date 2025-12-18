package com.manish.smartcart.controller;

import com.manish.smartcart.dto.AuthRequest;
import com.manish.smartcart.dto.CustomerAuthRequest;
import com.manish.smartcart.dto.LoginRequest;
import com.manish.smartcart.dto.SellerAuthRequest;
import com.manish.smartcart.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

     @Autowired
     private AuthService authService;


     //Register
    @PostMapping("/register/customer")
    public ResponseEntity<?>registerCustomer(@RequestBody CustomerAuthRequest authRequest){
        return authService.registerCustomer(authRequest);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<?>registerCustomer(@RequestBody SellerAuthRequest authRequest){
        return authService.registerSeller(authRequest);
    }

    //Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }



}
