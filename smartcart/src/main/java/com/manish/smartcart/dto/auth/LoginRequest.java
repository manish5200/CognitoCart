package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min=4 , message = "Password must have at least 4 characters")
    private String password;

}
