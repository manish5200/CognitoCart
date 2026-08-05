package com.manish.smartcart.user.service;

import com.manish.smartcart.user.dto.AddressRequest;
import com.manish.smartcart.user.dto.AddressResponse;
import com.manish.smartcart.user.model.Address;
import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.user.repository.AddressRepository;
import com.manish.smartcart.user.repository.UsersRepository;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core Domain Service for Address Book Management.
 * <p>
 * ARCHITECTURAL DESIGN:
 * 1. Edge Translation: Converts external UUIDs to internal Long IDs securely.
 * 2. SQL-Level Security: Ownership is verified directly in the WHERE clause, preventing IDOR leaks.
 * 3. Dirty Checking: Relies on Hibernate's managed state for updates, avoiding redundant save() calls.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {

        // We fetch the full User entity here because we need to potentially
        // mutate the user.setPrimaryAddress() shortcut.
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .streetAddress(request.getStreetAddress())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        // Business Rule: First address is always default, or explicit user request
        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;

        if (Boolean.TRUE.equals(request.getIsDefault()) || isFirstAddress) {
            handleDefaultToggle(userId);
            address.setIsDefault(true);
            user.setPrimaryAddress(address); // Keep the bidirectional sync intact
        } else {
            address.setIsDefault(false);
        }

        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, UUID addressPublicId, AddressRequest request) {

        // SECURITY OPTIMIZATION: Database-level ownership check.
        // If it belongs to another user, this returns empty. No IDOR leak.
        Address address = getAddressSecured(addressPublicId, userId);

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreetAddress(request.getStreetAddress());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            handleDefaultToggle(userId);
            address.setIsDefault(true);
            address.getUser().setPrimaryAddress(address);
        }

        // NOTE: No .save() needed! Hibernate dirty checking automatically issues
        // the UPDATE statement when this @Transactional method completes.
        return mapToResponse(address);
    }

    @Transactional
    public void deleteAddress(Long userId, UUID addressPublicId) {
        Address address = getAddressSecured(addressPublicId, userId);

        if (address.getIsDefault()) {
            address.getUser().setPrimaryAddress(null);
        }

        addressRepository.delete(address);
    }

    @Transactional
    public void setAsDefault(Long userId, UUID addressPublicId) {
        Address targetAddress = getAddressSecured(addressPublicId, userId);

        if (!targetAddress.getIsDefault()) {
            handleDefaultToggle(userId);
            targetAddress.setIsDefault(true);
            targetAddress.getUser().setPrimaryAddress(targetAddress);
        }
    }

    // ─── INTERNAL HELPERS ───────────────────────────────────────────────────

    /**
     * Reusable Edge Translation & Security Check.
     */
    private Address getAddressSecured(UUID addressPublicId, Long userId) {
        return addressRepository.findByPublicIdAndUserId(addressPublicId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found, or you do not have permission to modify it."));
    }

    private void handleDefaultToggle(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(oldDefault -> oldDefault.setIsDefault(false));
        // No .save() needed! Hibernate dirty checks the entity.
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                // CRITICAL: We return the UUID to the frontend, NEVER the Long ID.
                .publicAddressId(address.getPublicId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .streetAddress(address.getStreetAddress())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}