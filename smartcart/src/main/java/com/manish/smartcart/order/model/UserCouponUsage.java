package com.manish.smartcart.order.model;

import com.manish.smartcart.order.coupon.model.Coupon;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.user.model.Users;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_coupon_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id","coupon_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponUsage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Builder.Default
    private Integer usage = 0;
}
