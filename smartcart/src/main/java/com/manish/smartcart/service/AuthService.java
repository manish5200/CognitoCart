package com.manish.smartcart.service;

import com.manish.smartcart.config.jwt.JwtUtilService;
import com.manish.smartcart.dto.AuthRequest;
import com.manish.smartcart.dto.LoginRequest;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.Users;
import com.manish.smartcart.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtilService jwtService;

    //Register
    public ResponseEntity<?> register(AuthRequest authRequest) {
        try {
            String email = authRequest.getEmail() == null ? null : authRequest.getEmail();

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
            }

            if (usersRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "email already exists"));
            }

            Users newUser = new Users();
            newUser.setName(authRequest.getName().trim());
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(authRequest.getPassword()));
            newUser.setRole(authRequest.getRole() != null ? authRequest.getRole() : Role.CUSTOMER);
            Users savedUser = usersRepository.save(newUser);

            //don't expose hashed password
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", savedUser.getId());
            body.put("name", savedUser.getName());
            body.put("email", savedUser.getEmail());
            body.put("role", savedUser.getRole());
            body.put("createdAt", savedUser.getCreatedAt());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("Error in registering", e.getMessage()));
        }
    }

    //Login
   public ResponseEntity<?>login(LoginRequest loginRequest){
         try{
             Authentication authentication = authenticationManager.authenticate(
                     new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword()));

             if(authentication.isAuthenticated()){
                  String token = jwtService.generateToken(loginRequest.getEmail());
                  return ResponseEntity.status(HttpStatus.OK).body(Map.of(token,"status : 200"));
             }else{
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "username or password incorrect"));
             }

         } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in login", e.getMessage()));
         }
    }

}
