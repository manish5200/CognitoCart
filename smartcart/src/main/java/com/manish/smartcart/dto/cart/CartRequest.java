package com.manish.smartcart.dto.cart;

import jakarta.validation.constraints.Min;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CartRequest {

    private Long productId;
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

}
