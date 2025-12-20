package com.manish.smartcart.dto;

import com.manish.smartcart.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

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

    public SellerAuthRequest(String email, String password, String storeName, String businessAdder, String gstin, String panCard) {
        this.email = email;
        this.password = password;
        this.role = Role.SELLER.name();
        this.storeName = storeName;
        this.businessAdder = businessAdder;
        this.gstin = gstin;
        this.panCard = panCard;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBusinessAdder() {
        return businessAdder;
    }

    public void setBusinessAdder(String businessAdder) {
        this.businessAdder = businessAdder;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPanCard() {
        return panCard;
    }

    public void setPanCard(String panCard) {
        this.panCard = panCard;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
