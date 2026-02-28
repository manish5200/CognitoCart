package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.model.user.Address;
import com.manish.smartcart.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/addresses") // Versioned API
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<?> addAddress(@Valid @RequestBody Address addressRequest) {
        // In a real app, extract userId from the JWT token
        Long userId = getAuthenticatedUserId();
        Address address = addressService.addAddress(userId, addressRequest);
        return ResponseEntity.ok(Map.of("message", "Successfully added address", "Address", address));
    }

    @GetMapping
    public ResponseEntity<List<Address>> getMyAddresses() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<?> setPrimaryAddress(@PathVariable("addressId") Long addressId) {
        Long userId = getAuthenticatedUserId();
        addressService.setAsDefault(userId, addressId);
        return ResponseEntity.ok(Map.of("message", "Primary address updated successfully"));
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        assert customUserDetails != null;
        return customUserDetails.getUser().getId();
    }
}
