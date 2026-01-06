package com.manish.smartcart.dto.auth;

import com.manish.smartcart.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class AuthRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min=4 , message = "Password must have at least 4 characters")
    private String password;

    private Role role;
}
