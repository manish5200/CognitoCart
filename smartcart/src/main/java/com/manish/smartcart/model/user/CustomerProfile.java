package com.manish.smartcart.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="customer_profiles")
public class CustomerProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_customer_profile_user"))
    private Users user;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Size(max = 255)
    private String defaultShippingAddress;

    @Size(max = 255)
    private String defaultBillingAddress;

}
