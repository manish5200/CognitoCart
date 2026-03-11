package com.manish.smartcart.repository;

import com.manish.smartcart.model.order.UserCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserCouponUsageRepository extends JpaRepository<UserCouponUsage, Long> {
    Optional<UserCouponUsage> findByUserIdAndCouponId(Long userId, Long couponId);
}
