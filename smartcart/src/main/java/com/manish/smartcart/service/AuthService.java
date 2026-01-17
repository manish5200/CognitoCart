package com.manish.smartcart.service;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.config.jwt.JwtUtil;
import com.manish.smartcart.dto.auth.*;
import com.manish.smartcart.enums.ErrorCode;
import com.manish.smartcart.enums.Gender;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.CustomerProfile;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.util.PhoneUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * LOGIN: The "Gatekeeper"
     * Orchestrates authentication, JWT generation, and stateful session storage.
     * The "Heavy Payload" Login - Used once at session start.
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest){
        // 1. Authenticate (Throws BadCredentialsException if fails - no 'if' needed)
        Authentication authentication = authenticationManager.authenticate(
             new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword()));

        // Extract user from principal (No need to hit DB again)
        // 2. Extract User from Principal using CustomerUserDetails (Optimization: Avoids findByEmail call)
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Users user = userDetails.getUser();

        // 3. Generate Dual Tokens (Stateless + Stateful)
        String accessToken = jwtUtil.generateToken(user.getEmail());
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 4. Identity Hoisting (Return everything the UI needs in one trip)

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * REFRESH: The "Silent Maintainer"
     * Implements Token Rotation to mitigate session hijacking.
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request){
         return refreshTokenService.findByToken(request.getRefreshToken())
                 .map(refreshTokenService::verifyExpiration)
                 .map(token -> {
                    Users user = token.getUser();
                    String accessToken = jwtUtil.generateToken(user.getEmail());
                    // ROTATION: Old token is deleted inside createRefreshToken
                    var newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    return TokenRefreshResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .build();
                 })
                 .orElseThrow(()-> new RuntimeException("Refresh token is invalid or missing!"));

    }

    // -----------------------------------------
    // REGISTER CUSTOMER
    // -----------------------------------------
   @Transactional
   public void registerCustomer(CustomerAuthRequest request){
         // 1. Validate email
         if (usersRepository.existsByEmail(request.getEmail())) {
             throw new RuntimeException("Email already exists");
         }

        String normalizedPhone = PhoneUtil.normalize(request.getPhone(), "+91");
        if(usersRepository.existsByPhone(normalizedPhone)){
           throw new RuntimeException(String.valueOf(ErrorCode.PHONE_ALREADY_EXISTS));
        }

       // 2. Create User with Hoisted Identity Fields
        Users user  = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .fullName(request.getName())
                .phone(normalizedPhone)
                .dateOfBirth(request.getDateOfBirth())
                .gender(Gender.valueOf(request.getGender().toUpperCase()))
                .active(true)
                .build();

         // 2. Create Lean Customer Profile
         CustomerProfile profile = CustomerProfile.builder()
                         .user(user)
                                 .build();
         // 3. Link both sides (VERY IMPORTANT)
         user.setCustomerProfile(profile);
         // 4. Save User (Cascade persists profile)
         usersRepository.save(user);
}



//Register Seller
public void registerSeller(SellerAuthRequest request){
        // 1. Validate email
       if (usersRepository.existsByEmail(request.getEmail())) {
             throw new RuntimeException("Email already exists");
        }

        // 2. Create User with Hoisted Identity Fields
        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .fullName(request.getStoreName())
                .active(true)
                .build();


        // 3. Create Seller Profile and link both sides (VERY IMPORTANT)
        user.setSellerProfile(SellerProfile.builder()
                .user(user) // Link it with the user
                .storeName(request.getStoreName())
                .businessAddress(request.getBusinessAdder())
                .gstin(request.getGstin())
                .panCard(request.getPanCard())
                .build());
        // 4. Save only user (cascade persists profile)
       usersRepository.save(user);
        }
}
