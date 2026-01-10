package com.manish.smartcart.service;

import com.manish.smartcart.model.user.Address;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.AddressRepository;
import com.manish.smartcart.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public Address addAddress(Long userId, Address addressRequest) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //If this is the first address or set as default
        if(addressRequest.getIsDefault() || user.getPrimaryAddress() == null) {
            handleDefaultToggle(userId);
            addressRequest.setIsDefault(true);
            user.setPrimaryAddress(addressRequest);// Update logistics shortcut
        }
        addressRequest.setUser(user);
        return addressRepository.save(addressRequest);
    }

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

    private void handleDefaultToggle(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(oldDefault -> {
                    oldDefault.setIsDefault(false);
                    addressRepository.save(oldDefault);
        });
    }

    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId);
    }
}
