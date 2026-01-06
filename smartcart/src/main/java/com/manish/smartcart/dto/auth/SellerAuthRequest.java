package com.manish.smartcart.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class SellerAuthRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be in valid format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min =4, message = "Password must be at least 4 characters")
    private String password;

    private String role;

    @NotBlank(message = "Store name is required")
    @Size(max = 255)
    private String storeName;

    @NotBlank(message = "Business address is required")
    private String businessAdder;

    @UniqueElements(message = "GST number should be unique")
    private String gstin;

    private String panCard;

}
