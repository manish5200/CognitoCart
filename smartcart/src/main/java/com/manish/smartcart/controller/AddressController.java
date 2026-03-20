package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.users.AddressRequest;
import com.manish.smartcart.dto.users.AddressResponse;
import com.manish.smartcart.model.user.Address;
import com.manish.smartcart.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/addresses") // Versioned API
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "Manage user delivery addresses (Address Book)")
public class AddressController {

    private final AddressService addressService;

    // ─── POST: Add Address
    @PostMapping
    @Operation(summary = "Add a new address")
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody
                                                          AddressRequest request) {
        // In a real app, extract userId from the JWT token
        Long userId = getAuthenticatedUserId();
        AddressResponse address = addressService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    // ─── GET: List Addresses
    @GetMapping
    @Operation(summary = "Get all addresses for authenticated user")
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    // ─── PUT: Update Address
    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing address")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request){
        Long userId = getAuthenticatedUserId();
        AddressResponse address = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(address);
    }

    // ─── DELETE: Delete Address
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long addressId) {

        Long userId = getAuthenticatedUserId();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }

    // ─── PATCH: Set as Default
    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Set a specific address as the default primary address")
    public ResponseEntity<?> setPrimaryAddress(
            @PathVariable Long addressId) {
        Long userId = getAuthenticatedUserId();
        addressService.setAsDefault(userId, addressId);
        return ResponseEntity.ok(Map.of("message", "Primary address updated successfully"));
    }

    //Helper method
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        assert customUserDetails != null;
        return customUserDetails.getUser().getId();
    }
}
