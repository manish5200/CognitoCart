package com.manish.smartcart.dto.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * RegisterResponse — Returned after a successful customer or seller
 * registration.
 * Gives the frontend/client enough context to act immediately (e.g. redirect to
 * KYC notice page for sellers, or straight to login for customers).
 */
@Getter
@Builder
public class RegisterResponse {

    private String message;
    private String email;
    private String role;

    /**
     * KYC status of the newly registered account.
     * - null for CUSTOMER (no KYC required)
     * - "PENDING" for SELLER (docs not yet submitted)
     */
    private String kycStatus;
}
