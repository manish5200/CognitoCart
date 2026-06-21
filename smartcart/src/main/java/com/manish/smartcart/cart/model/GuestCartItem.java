package com.manish.smartcart.cart.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestCartItem implements Serializable {

    private Long variantId;
    private Integer quantity;
    private BigDecimal priceAtAdding;
}
