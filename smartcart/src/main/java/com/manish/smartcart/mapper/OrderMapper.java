package com.manish.smartcart.mapper;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Makes it a Spring-managed bean
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(order.getId());
        orderResponse.setOrderDate(order.getOrderDate());
        orderResponse.setTotalAmount(order.getTotal());
        orderResponse.setStatus(order.getOrderStatus());
        orderResponse.setShippingAddress(order.getShippingAddress());

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
                orderItem.getPriceAtPurchase()
        );
    }
}
