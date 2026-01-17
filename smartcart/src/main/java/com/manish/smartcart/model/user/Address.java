package com.manish.smartcart.model.user;

import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SoftDelete(columnName = "is_deleted") // Preserves order history records
@SuperBuilder
@Entity
@Table(name = "user_addresses")
public class Address extends BaseEntity{

    @NotBlank(message = "Recipient name is required")
    private String fullName; // Name of the person receiving the package

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber; // Delivery contact number

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

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false; // Flag for UI "Default" selection

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // Links address to the core User account

    // Helper to link the user properly
    public void setUser(Users user) {
        this.user = user;
        // If the user currently has no primary address, this becomes it
        if (user.getPrimaryAddress() == null) {
            user.setPrimaryAddress(this);
            this.isDefault = true;
        }
    }
}

