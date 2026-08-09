package com.manish.smartcart.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Valid
    private ShippingAddressRequest shippingAddress;

    // Optional: number of loyalty points to redeem for this order.
    // Each 100 points = ₹10 discount.
    @Min(0)
    @Builder.Default
    private Integer redeemLoyaltyPoints = 0;

}
