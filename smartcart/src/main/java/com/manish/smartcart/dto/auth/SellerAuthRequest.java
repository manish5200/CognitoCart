package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SellerAuthRequest {

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be in valid format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Store name is required")
    @Size(max = 255)
    private String storeName;

    @NotBlank(message = "Business address is required")
    @Size(min = 10, message = "Business address must be at least 10 characters")
    private String businessAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone number format")
    private String phone;

    // Optional at registration — submitted separately during KYC
    @Size(max = 15, message = "GSTIN must not exceed 15 characters")
    private String gstin;

    private String panCard;
}
