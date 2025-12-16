package com.manish.smartcart.config;

import com.manish.smartcart.model.Users;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

public class UsersUserDetails implements UserDetails {

    private String email; //will be used as username
    private String password;
    private List<GrantedAuthority>authorities;

    public UsersUserDetails(Users users) {
        this.email = users.getEmail();
        this.password = users.getPassword();
        String role = users.getRole().name();
        String prefixedRole = "ROLE_" + role;
        this.authorities = List.of(new SimpleGrantedAuthority(prefixedRole));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
//        return UserDetails.super.isAccountNonExpired();
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        //return UserDetails.super.isAccountNonLocked();
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        //return UserDetails.super.isCredentialsNonExpired();
        return true;
    }

    @Override
    public boolean isEnabled() {
       // return UserDetails.super.isEnabled();
        return true;
    }
}
