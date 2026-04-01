package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.users.*;
import com.manish.smartcart.service.AddressService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/v1/addresses") // Versioned API
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "Manage user delivery addresses (Address Book)")
public class AddressController {

    private final AddressService addressService;

    // ─── POST: Add Address
    @PostMapping
    @Operation(summary = "Add a new address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Address created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
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
    @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    // ─── PUT: Update Address
    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Address updated successfully"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long addressId) {

        Long userId = getAuthenticatedUserId();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }

    // ─── PATCH: Set as Default
    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Set a specific address as the default primary address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Primary address updated successfully"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
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
