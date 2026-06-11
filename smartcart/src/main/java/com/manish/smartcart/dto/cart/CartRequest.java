package com.manish.smartcart.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CartRequest {

    // The specific purchasable SKU the customer selected (e.g., Red / Size L).
    // ProductVariant ID — required. The variant knows its parent product.
    @NotNull(message = "variantId is required")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

}
