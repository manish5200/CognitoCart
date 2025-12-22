package com.manish.smartcart.dto.auth;

import com.manish.smartcart.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public CustomerAuthRequest() {
    }

    public CustomerAuthRequest(String name, String email, String password, String role, String phone, String billingAdder, String shippingAdder) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.CUSTOMER.toString();
        this.phone = phone;
        this.billingAdder = billingAdder;
        this.shippingAdder = shippingAdder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBillingAdder() {
        return billingAdder;
    }

    public void setBillingAdder(String billingAdder) {
        this.billingAdder = billingAdder;
    }

    public String getShippingAdder() {
        return shippingAdder;
    }

    public void setShippingAdder(String shippingAdder) {
        this.shippingAdder = shippingAdder;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
