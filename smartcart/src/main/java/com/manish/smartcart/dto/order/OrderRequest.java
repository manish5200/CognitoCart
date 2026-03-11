package com.manish.smartcart.dto.order;

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
