package com.manish.smartcart.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    private String landmark;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^\\d{5,6}$", message = "Invalid Zip/Pin code")
    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;
}
