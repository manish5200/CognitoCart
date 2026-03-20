package com.manish.smartcart.service;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.config.jwt.JwtUtil;
import com.manish.smartcart.dto.auth.*;
import com.manish.smartcart.enums.ErrorCode;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.CustomerProfile;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.util.PhoneUtil;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final TokenBlacklistService tokenBlacklistService; //for log out
    private final OtpService otpService;
    private final GuestCartService guestCartService;

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------
    /**
     * Orchestrates authentication, JWT generation, and stateful session storage.
     * "Heavy Payload" login - used once at session start.
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. Authenticate — throws BadCredentialsException on failure (no 'if' needed)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        // 2. Extract User from Principal (optimization: avoids a second DB call)
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Users user = userDetails.getUser();

        // 3. Generate Dual Tokens (stateless access + stateful refresh)
        String accessToken = jwtUtil.generateToken(user.getEmail());
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // NEW: Merger Trigger
        if(loginRequest.getGuestSessionId() != null &&
        !loginRequest.getGuestSessionId().isBlank()){
            guestCartService.mergeCart(loginRequest.getGuestSessionId(), user.getId());
        }

        // 4. Identity Hoisting — return everything the UI needs in one trip
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    // -----------------------------------------
    // TOKEN REFRESH
    // -----------------------------------------

    /**
     * Implements Token Rotation to mitigate session hijacking.
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    Users user = token.getUser();
                    String accessToken = jwtUtil.generateToken(user.getEmail());
                    // Rotation: old token deleted inside createRefreshToken
                    var newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    return TokenRefreshResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is invalid or missing!"));
    }

    // -----------------------------------------
    // REGISTER CUSTOMER
    // -----------------------------------------

    @Transactional
    public RegisterResponse registerCustomer(CustomerAuthRequest request) {
        // 1. Duplicate checks
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("An account with this email already exists.");
        }

        String normalizedPhone = PhoneUtil.normalize(request.getPhone(), "+91");
        if (usersRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalStateException(ErrorCode.PHONE_ALREADY_EXISTS.name());
        }

        // 2. Create User — gender is now a proper enum field (no manual parsing needed)
        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .fullName(request.getName())
                .phone(normalizedPhone)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender()) // Gender enum — Jackson validates this automatically
                .active(true)
                .build();

        // 3. Create lean Customer Profile and link both sides
        CustomerProfile profile = CustomerProfile.builder()
                .user(user)
                .build();
        user.setCustomerProfile(profile);

        // 4. Save user (cascade persists profile)
        usersRepository.save(user);

        // 5. Send Welcome Email
        try {
            String body = emailTemplateBuilder.buildWelcomeEmail(user);
            emailService.sendMail(user.getEmail(), "Welcome to CognitoCart! \uD83C\uDF89", body, "CognitoCart Team");
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", user.getEmail(), e);
        }

        // Send email verification OTP immediately after registration.
        // The account is active but checkout is blocked until email is verified.
        try {
            String otp = otpService.generateAndStore(user.getEmail());
            String body = emailTemplateBuilder.buildEmailVerificationEmail(user, otp);
            emailService.sendMail(user.getEmail(),
                    "Verify Your CognitoCart Email ✉️", body, "CognitoCart Team");
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}", user.getEmail(), e);
        }

        // NEW: Merger Trigger
        if (request.getGuestSessionId() != null && !request.getGuestSessionId().isBlank()) {
            guestCartService.mergeCart(request.getGuestSessionId(), user.getId());
        }

        return RegisterResponse.builder()
                .message("Customer registered successfully. Welcome to CognitoCart! ✅")
                .email(user.getEmail())
                .role(Role.CUSTOMER.name())
                .kycStatus(null) // Customers have no KYC requirement
                .build();
    }

    // -----------------------------------------
    // REGISTER SELLER
    // -----------------------------------------

    @Transactional // FIX: was missing — without this, a save failure left partial state
    public RegisterResponse registerSeller(SellerAuthRequest request) {
        // 1. Duplicate checks
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("An account with this email already exists.");
        }

        String normalizedPhone = PhoneUtil.normalize(request.getPhone(), "+91");
        if (usersRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalStateException(ErrorCode.PHONE_ALREADY_EXISTS.name());
        }

        // 2. Create User
        // FIX: fullName now correctly uses ownerName (the real person), NOT storeName
        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .fullName(request.getOwnerName()) // FIX: was request.getStoreName()
                .phone(normalizedPhone)
                .active(true)
                .build();

        // 3. Create Seller Profile and link both sides
        // FIX: businessAdder typo corrected to businessAddress in DTO
        SellerProfile sellerProfile = SellerProfile.builder()
                .user(user)
                .storeName(request.getStoreName())
                .businessAddress(request.getBusinessAddress()) // FIX: was getBusinessAdder()
                .gstin(request.getGstin())
                .panCard(request.getPanCard())
                // kycStatus defaults to KycStatus.PENDING via @Builder.Default in entity
                .build();

        user.setSellerProfile(sellerProfile);

        // 4. Save user (cascade persists profile)
        usersRepository.save(user);

        // Send email verification OTP immediately after registration.
        // The account is active but checkout is blocked until email is verified.
        try {
            String otp = otpService.generateAndStore(user.getEmail());
            String body = emailTemplateBuilder.buildEmailVerificationEmail(user, otp);
            emailService.sendMail(user.getEmail(),
                    "Verify Your CognitoCart Email ✉️", body, "CognitoCart Team");
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}", user.getEmail(), e);
        }

        return RegisterResponse.builder()
                .message("Seller account created successfully. Your KYC verification is pending. ✅")
                .email(user.getEmail())
                .role(Role.SELLER.name())
                .kycStatus("PENDING") // Inform frontend immediately so it can show KYC notice
                .build();
    }

    // -----------------------------------------
    // LOGOUT
    // -----------------------------------------
/*
 * Logout = invalidate the current access token by adding its jti to Redis.
 * The refresh token is also deleted from the DB so the user can't
 * get a new access token without logging in again.
 * @Param authorizationHeader  The raw "Authorization: Bearer <token>" header
 */

    public void logout(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No valid Authorization header provided.");
        }

        String accessToken = authorizationHeader.substring(7);

        // 1. Blacklist the access token in Redis
        String jti = jwtUtil.extractJti(accessToken);
        long remainingTtl = jwtUtil.getRemainingTtlSeconds(accessToken);
        tokenBlacklistService.blacklist(jti, remainingTtl);

        // 2. Delete the refresh token from DB so rotation is broken
        String email = jwtUtil.extractUsername(accessToken);

        usersRepository.findByEmail(email).ifPresent(user ->
                refreshTokenService.deleteByUserId(user.getId())
        );

        log.info("✅ User logged out successfully: {}", email);
    }


}
