package com.manish.smartcart.model.user;

import com.manish.smartcart.enums.KycStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seller_profiles")
public class SellerProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
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
