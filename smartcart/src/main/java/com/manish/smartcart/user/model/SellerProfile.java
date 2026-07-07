package com.manish.smartcart.user.model;

import com.manish.smartcart.shared.enums.KycStatus;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Stores vendor-specific business and regulatory details.
 * <p>
 * DESIGN NOTE: Shares a Primary Key with the Users entity via @MapsId.
 * This guarantees a strict 1:1 relationship at the database level and eliminates
 * the need for a separate sequence generator.
 */
@Entity
@Table(name = "seller_profiles")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
public class SellerProfile extends BaseEntity {

    /**
     * Customer-facing or support-facing vendor ID (e.g., SEL-YYYYMMDD-XXXXXX).
     */
    @Column(name = "seller_code", unique = true, nullable = false, length = 33)
    private String sellerCode;

    /**
     * The master user account. The ID of this user becomes the ID of this profile.
     */
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_seller_profile_user"))
    private Users user;

    @Column(nullable = false, length = 120)
    private String storeName;

    @Size(min = 10)
    private String businessAddress;

    /**
     * Goods and Services Tax Identification Number (India).
     * Used for vendor payouts and B2B taxation compliance.
     */
    @Column(unique = true, length = 15)
    private String gstin;

    private String panCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    /**
     * Generates the human-readable seller code before the first DB insert.
     */
    @PrePersist
    private void humanIDGenerator() {
        if(this.sellerCode == null){
            this.sellerCode = HumanIdGenerator.generate("SEL");
        }
    }
}