package com.manish.smartcart.repository;

import com.manish.smartcart.model.order.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);
}
