package com.manish.smartcart.user.model;

import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Stores shopper-specific preferences and marketing metrics.
 * <p>
 * DESIGN NOTE: Shares a Primary Key with the Users entity via @MapsId.
 * Isolated from the main Users table so seller accounts do not carry empty shopper fields.
 */
@Entity
@Table(name = "customer_profiles")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
public class CustomerProfile extends BaseEntity {

    /**
     * The master user account. The ID of this user becomes the ID of this profile.
     */
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_customer_profile_user"))
    private Users user;

    /**
     * Platform currency used for discounts and gamification.
     */
    @Builder.Default
    private Integer loyaltyPoints = 0;

    /**
     * JSON payload or comma-separated list of product categories the user prefers.
     */
    @Size(max = 500)
    private String preferences;

}