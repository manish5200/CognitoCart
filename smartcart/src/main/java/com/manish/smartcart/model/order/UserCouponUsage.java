package com.manish.smartcart.model.order;

import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.user.Users;
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
