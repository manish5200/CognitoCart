package com.manish.smartcart.model;

import com.manish.smartcart.enums.KycStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

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

    public SellerProfile() {}

    public SellerProfile(String storeName, String gstin) {
        this.storeName = storeName;
        this.gstin = gstin;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String adder) { this.businessAddress = adder; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getPanCard() { return panCard; }
    public void setPanCard(String panCard) { this.panCard = panCard; }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus status) { this.kycStatus = status; }
}
