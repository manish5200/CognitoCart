package com.manish.smartcart.dto.cart;

import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponse {
    private Long cartId;
    private BigDecimal totalAmount;
    private List<ItemDTO> items;

    public CartResponse() {}

    public CartResponse(Long cartId, BigDecimal totalAmount, List<ItemDTO> items) {
        this.cartId = cartId;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<ItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemDTO> items) {
        this.items = items;
    }

    // A simple Item DTO with NO link back to cart
    public static class ItemDTO {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        public ItemDTO(String productName, BigDecimal price, Integer quantity, BigDecimal subtotal) {
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    //Helper method
    public CartResponse getCartResponse(Cart updatedCart) {
        List<ItemDTO>items = new ArrayList<>();
        for(CartItem item : updatedCart.getItems()){
            ItemDTO newItem = new ItemDTO(
                    item.getProduct().getProductName(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getPrice().multiply(new BigDecimal(item.getQuantity()))
            );
            items.add(newItem);
        }
        CartResponse cartResponse = new CartResponse(
                updatedCart.getId(),
                updatedCart.getTotalAmount(),
                items);
        return cartResponse;
    }
}
