package com.manish.smartcart.dto.cart;

import jakarta.validation.constraints.Min;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CartRequest {

    private Long productId;
    @Min(value = 0)
    private Integer quantity;

}
