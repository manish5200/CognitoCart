package com.manish.smartcart.model.user;

import com.manish.smartcart.enums.KycStatus;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seller_profiles")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
public class SellerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // User ID becomes the Profile ID
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_seller_profile_user"))
    private Users user;

    @Column(nullable = false, length = 120)
    private String storeName;

    @Size(min = 10)
    private String businessAddress;

    @Column(unique = true, length = 15)
    private String gstin;

    private String panCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;
}
