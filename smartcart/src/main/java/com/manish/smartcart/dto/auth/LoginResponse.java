package com.manish.smartcart.dto.auth;

import lombok.*;

/**
 * LoginResponse: Used ONLY during /api/auth/login.
 * Provides a complete identity snapshot for the frontend.
 **/

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class LoginResponse {
    private String accessToken;  // Stateless JWT
    private String refreshToken; // Database-backed token

    @Builder.Default
    private String tokenType = "Bearer";

    //Identity Hoisting (UX Optimization)
    private Long userId;
    private String email;
    private String fullName;
    private String role; // e.g., "ROLE_CUSTOMER" or "ROLE_SELLER"

    // --- SESSION METADATA (Optional but helpful for Frontend) ---
    private Long accessTokenExpiryMillis; // Tells the frontend exactly when to refresh
}
