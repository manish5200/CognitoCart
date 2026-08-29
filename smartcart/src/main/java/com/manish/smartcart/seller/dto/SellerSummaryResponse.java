package com.manish.smartcart.seller.dto;

import com.manish.smartcart.shared.enums.KycStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Data
@Builder
@AllArgsConstructor
public class SellerSummaryResponse {
    private UUID sellerPublicId;
    private String sellerCode; // Human ID
    private Long sellerId;
    private String fullName;
    private String email;
    private String storeName;
    private String gstin;
    private KycStatus kycStatus;
    private LocalDateTime registeredAt;
}
