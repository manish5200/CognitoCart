package com.manish.smartcart.order.dto;

import jakarta.validation.Valid;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @Valid
    private ShippingAddressRequest shippingAddress;
}
