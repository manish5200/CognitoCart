package com.manish.smartcart.dto.cart;

import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CartResponse {
    private Long cartId;
    private BigDecimal totalAmount;
    private List<ItemDTO> items;

    // A simple Item DTO with NO link back to cart
    @Setter
    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class ItemDTO {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

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
        return new CartResponse(
                updatedCart.getId(),
                updatedCart.getTotalAmount(),
                items);
    }
}
