package com.manish.smartcart.dto.auth;

public class LoginResponse {

    private String token;
    private int status;
    private String role;

    public LoginResponse(String token, int status, String role) {
        this.token = token;
        this.status = status;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
