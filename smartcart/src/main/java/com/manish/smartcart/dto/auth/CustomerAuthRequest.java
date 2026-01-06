package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CustomerAuthRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(message = "Password must me at least 4 characters")
    private String password;

    private String role;

    private String phone;

    private String shippingAdder;

    private String billingAdder;
}
