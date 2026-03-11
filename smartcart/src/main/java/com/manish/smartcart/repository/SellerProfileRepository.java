package com.manish.smartcart.repository;

import com.manish.smartcart.model.user.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    // ID = user_id (shared PK via @MapsId on SellerProfile)
}
