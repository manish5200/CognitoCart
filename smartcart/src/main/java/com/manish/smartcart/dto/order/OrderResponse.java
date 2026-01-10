package com.manish.smartcart.dto.order;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.user.Address;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String email;
    private String customerName;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    //List of the items we ordered
    private List<OrderItemDTO> items;

    @Setter
    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class OrderItemDTO{
        // Getters and Setters
        private String productName;
        private Integer quantity;
        private BigDecimal priceAtPurchase; // The "Frozen" price
        private BigDecimal subtotal;        // quantity * priceAtPurchase

        public OrderItemDTO(String productName, Integer quantity, BigDecimal priceAtPurchase) {
            this.productName = productName;
            this.quantity = quantity;
            this.priceAtPurchase = priceAtPurchase;
            // Automatically calculate subtotal for the frontend
            this.subtotal = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
        }

    }

}
