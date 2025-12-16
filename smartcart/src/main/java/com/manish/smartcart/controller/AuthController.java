package com.manish.smartcart.controller;

import com.manish.smartcart.dto.AuthRequest;
import com.manish.smartcart.dto.LoginRequest;
import com.manish.smartcart.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

     @Autowired
     private AuthService authService;


     //Register
    @PostMapping("/register")
    public ResponseEntity<?>signUp(@RequestBody AuthRequest authRequest){
        return authService.register(authRequest);
    }

    //Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }



}
