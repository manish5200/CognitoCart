package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class ForgotPasswordRequest{
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;
}
