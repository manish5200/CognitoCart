package com.manish.smartcart.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public class CartRequest {

    private Long productId;
    @Min(value = 0)
    private Integer quantity;

    public CartRequest() {

    }
    public CartRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
