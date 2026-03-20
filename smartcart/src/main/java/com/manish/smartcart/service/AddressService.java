package com.manish.smartcart.service;

import com.manish.smartcart.dto.users.AddressRequest;
import com.manish.smartcart.dto.users.AddressResponse;
import com.manish.smartcart.model.user.Address;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.AddressRepository;
import com.manish.smartcart.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UsersRepository usersRepository;

    // ─── CREATE ─────────────────────────────────────────────────────────────
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Map DTO to Entity
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
        // If this is the user's first address, naturally force it as default
        if(request.getIsDefault() || user.getPrimaryAddress() == null) {
            handleDefaultToggle(userId); // Unset old default
            request.setIsDefault(true);
            user.setPrimaryAddress(address);// Sync Users shortcut
        }
        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    // ─── READ ───────────────────────────────────────────────────────────────
    public List<AddressResponse>getUserAddresses(Long userId) {
        // Fetch all addresses for this user, map them to DTOs
        List<Address>addresses = addressRepository.findByUserId(userId);

        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
         Address address = addressRepository.findById(addressId)
                 .orElseThrow(() -> new RuntimeException("Address not found"));

        // Security check: Never let User A edit User B's address!
        if(!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to modify this address");
        }
        // Update fields
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreetAddress(request.getStreetAddress());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());

        // Handle default toggle logic if user ticked the box
        if(Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            handleDefaultToggle(userId);
            address.setIsDefault(true);
            address.getUser().setPrimaryAddress(address);
        }
        Address updatedAddress = addressRepository.save(address);
        return mapToResponse(updatedAddress);
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if(!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this address");
        }
        // What if they delete their primary address?
        if (address.getIsDefault()) {
            address.getUser().setPrimaryAddress(null); // Clear the shortcut
        }

        // We call delete(). Because your Entity has @SoftDelete, Hibernate
        // will safely set is_deleted = true in the DB. Order history won't break!
        addressRepository.delete(address);
    }

    // ─── SET PRIMARY ────────────────────────────────────────────────────────
    @Transactional
    public void setAsDefault(Long userId, Long addressId) {
        Address targetAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Security check: ensure address belongs to the user
        if (!targetAddress.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized address access");
        }

        handleDefaultToggle(userId); // Unset the previous default
        targetAddress.setIsDefault(true);
        targetAddress.getUser().setPrimaryAddress(targetAddress); // Sync shortcut
        addressRepository.save(targetAddress);
    }
    // ─── HELPERS ────────────────────────────────────────────────────────────
    private void handleDefaultToggle(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(oldDefault -> {
                    oldDefault.setIsDefault(false);
                    addressRepository.save(oldDefault);
                });
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
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
