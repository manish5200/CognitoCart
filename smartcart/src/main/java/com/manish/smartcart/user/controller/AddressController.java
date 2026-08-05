package com.manish.smartcart.user.controller;

import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.user.dto.AddressRequest;
import com.manish.smartcart.user.dto.AddressResponse;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.user.service.AddressService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

/**
 * Pure Edge API Gateway for Address Book Management.
 *
 * SECURITY ADHERENCE:
 * - IDOR Prevention: All external address identifiers must be unguessable UUIDs.
 * - Architectural Purity: Delegates UUID-to-Long surrogate key translation to the Service layer.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "Manage user delivery addresses (Address Book)")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Add a new address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        AddressResponse address = addressService.addAddress(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @GetMapping
    @Operation(summary = "Get all addresses for authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable("addressId") UUID addressPublicId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication){

        Long userId = extractUserId(authentication);

        // Pure Delegation: Service layer handles the UUID resolution and ownership validation
        AddressResponse address = addressService.updateAddress(userId, addressPublicId, request);

        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<?> deleteAddress(
            @PathVariable("addressId") UUID addressPublicId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        addressService.deleteAddress(userId, addressPublicId);

        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }

    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Set a specific address as the default primary address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primary address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<?> setPrimaryAddress(
            @PathVariable("addressId") UUID addressPublicId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        addressService.setAsDefault(userId, addressPublicId);

        return ResponseEntity.ok(Map.of("message", "Primary address updated successfully"));
    }

    /**
     * Security Context Extractor.
     * Replaces unsafe `assert` logic with explicit exception handling.
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessLogicException("Authentication context is missing or invalid. Please log in again.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}