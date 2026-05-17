package com.manish.smartcart.repository;

import com.manish.smartcart.enums.KycStatus;
import com.manish.smartcart.model.user.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    // ID = user_id (shared PK via @MapsId on SellerProfile)
    // JOIN FETCH prevents N+1: loads seller + user in ONE SQL query
    @Query("SELECT sp FROM SellerProfile sp JOIN FETCH sp.user u " +
            "WHERE sp.kycStatus IN :statuses ORDER BY sp.createdAt ASC")
    List<SellerProfile> findByKycStatusIn(@Param("statuses") List<KycStatus> statuses);

    @Query("SELECT sp FROM SellerProfile sp JOIN FETCH sp.user u ORDER BY sp.createdAt DESC")
    List<SellerProfile> findAllWithUser();
}
