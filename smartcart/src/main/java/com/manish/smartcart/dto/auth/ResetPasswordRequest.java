package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class ResetPasswordRequest {

    // The UUID token from the email link
    @NotBlank(message = "Reset token is required")
    private String token;

    // New password — must meet minimum strength
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
