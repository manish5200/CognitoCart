package com.manish.smartcart.order.repository;

import com.manish.smartcart.order.model.UserCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserCouponUsageRepository extends JpaRepository<UserCouponUsage, Long> {
    Optional<UserCouponUsage> findByUserIdAndCouponId(Long userId, Long couponId);
}
