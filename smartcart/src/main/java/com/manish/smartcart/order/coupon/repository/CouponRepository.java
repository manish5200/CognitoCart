package com.manish.smartcart.order.coupon.repository;

import com.manish.smartcart.order.coupon.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);

    // PUBLIC ID LOOKUP
    Optional<Coupon> findByPublicId(UUID publicId);
}
