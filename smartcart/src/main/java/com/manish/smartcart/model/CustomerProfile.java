package com.manish.smartcart.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
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

    @ElementCollection
    @CollectionTable(name = "wishlist", joinColumns = @JoinColumn(name = "customer_id"),
            foreignKey = @ForeignKey(name = "fk_wishlist_customer"))
    @Column(name = "product_id")
    private Set<Long> likedProductIds = new HashSet<>();

    public CustomerProfile() {
    }

    public CustomerProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDefaultShippingAddress() { return defaultShippingAddress; }
    public void setDefaultShippingAddress(String adder) { this.defaultShippingAddress = adder; }
    public String getDefaultBillingAddress() { return defaultBillingAddress; }
    public void setDefaultBillingAddress(String adder) { this.defaultBillingAddress = adder; }
    public Set<Long> getLikedProductIds() { return likedProductIds; }
    public void setLikedProductIds(Set<Long> likedProductIds) { this.likedProductIds = likedProductIds; }
}
