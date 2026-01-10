package com.manish.smartcart.repository;

import com.manish.smartcart.model.user.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Retrieves all active addresses for a specific user
    List<Address> findByUserId(Long userId);

    // Finds the current default address to assist in the "Atomic Toggle"
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
}
