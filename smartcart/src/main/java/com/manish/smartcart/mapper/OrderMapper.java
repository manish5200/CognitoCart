package com.manish.smartcart.mapper;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(order.getId());
        orderResponse.setEmail(order.getUser().getEmail());
        orderResponse.setCustomerName(order.getUser().getFullName()); // Uses hoisted name
        orderResponse.setOrderDate(order.getOrderDate());
        orderResponse.setTotalAmount(order.getTotalAmount());
        orderResponse.setStatus(order.getOrderStatus());

        // --- FIXED: Mapping the Snapshot Address ---
        // We build a readable address string from the individual snapshot columns
        String fullShippingInfo = String.format("%s (%s), %s, %s, %s - %s, %s",
                order.getShippingFullName(),
                order.getShippingPhone(),
                order.getShippingStreetAddress(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingZipCode(),
                order.getShippingCountry());

        orderResponse.setShippingAddress(fullShippingInfo);

        List<OrderResponse.OrderItemDTO> itemDTOs = order.getOrderItems()
                .stream()
                .map(this::toItemDTO)
                .toList();

        orderResponse.setItems(itemDTOs);

        return orderResponse;
    }

    private OrderResponse.OrderItemDTO toItemDTO(OrderItem orderItem) {
        return new OrderResponse.OrderItemDTO(
                orderItem.getProduct().getProductName(),
                orderItem.getQuantity(),
                orderItem.getPriceAtPurchase() // Correctly uses the "Frozen" price
        );
    }
}