package com.manish.smartcart.dto.users;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String streetAddress;
    private String landmark;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean isDefault;

    // Helper for frontend to easily render
    public String getFormattedAddress() {
        String base = String.format("%s, %s, %s - %s, %s",
                streetAddress, city, state, zipCode, country);
                
        if (landmark == null || landmark.isBlank()) {
            return base;
        }
        
        // Smart check: don't prepend "Near" if the user already typed it!
        String prefix = landmark.toLowerCase().startsWith("near") ? "" : "Near ";
        return prefix + landmark + ", " + base;
    }
}
