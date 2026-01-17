package com.manish.smartcart.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenRefreshResponse: Used ONLY during /api/auth/refresh.
 * Minimal payload to keep the background process fast and secure.
 **/

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken; // For Rotation: Send a brand new one

    @Builder.Default
    private String tokenType = "Bearer";
}
