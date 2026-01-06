package com.manish.smartcart.dto.order;

import com.manish.smartcart.model.order.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderRequest {

    @NotNull(message = "Shipping address is required")
    @Valid // This triggers the @NotBlank checks inside your Address class
    private Address shippingAddress;

    // You can add more fields here later, e.g., String paymentId;

}


/*
JSON for OderRequest
{
    "shippingAddress": {
        "recipientName": "Manish Chauhan",
        "phone": "XXXXXXXXXX",
        "street": "123 Tech Park",
        "city": "Prayagraj",
        "state": "UP",
        "postalCode": "211001",
        "country": "India"
    }
}
* */