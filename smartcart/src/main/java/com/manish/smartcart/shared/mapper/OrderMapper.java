package com.manish.smartcart.shared.mapper;

import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.order.dto.ShipmentTrackingDTO;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.OrderItem;
import com.manish.smartcart.order.model.Shipment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderPublicId(order.getPublicId());
        orderResponse.setEmail(order.getUser().getEmail());
        orderResponse.setCustomerName(order.getUser().getFullName()); // Uses hoisted name
        orderResponse.setCustomerPublicId(order.getUser().getPublicId());
        orderResponse.setShippingPhone(order.getShippingPhone());
        orderResponse.setOrderDate(order.getOrderDate());
        orderResponse.setTotalAmount(order.getTotalAmount());
        orderResponse.setCouponCode(order.getCouponCode());
        orderResponse.setDiscountAmount(order.getDiscountAmount());
        orderResponse.setStatus(order.getOrderStatus());
        orderResponse.setPaymentStatus(order.getPaymentStatus());
        orderResponse.setDeliveryFee(order.getDeliveryFee()); // ← was missing — fixes "null" in PDF invoice
        orderResponse.setReturnRequestType(order.getReturnRequestType());
        orderResponse.setReturnReason(order.getReturnReason());
        orderResponse.setReturnRequestedAt(order.getReturnRequestedAt());

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
                orderItem.getProductNameSnapshot(),
                // Snapshot is frozen at checkout — always correct even if product is renamed/deleted.
                // InvoiceService reads this via OrderItemDTO.productName → no PDF change needed.
                orderItem.getQuantity(),
                orderItem.getPriceAtPurchase() // Correctly uses the "Frozen" price
        );
    }

    // Add this helper method to OrderMapper.java
    public void mapShipment(OrderResponse response, Shipment shipment) {
        // CONCEPT: Called after toOrderResponse() to inject tracking data if available.
        // Nullable by design — most orders won't have a shipment yet.
        if (shipment != null) {
            response.setShipmentTracking(ShipmentTrackingDTO.builder()
                    .courierName(shipment.getCourierName())
                    .trackingNumber(shipment.getTrackingNumber())
                    .trackingUrl(shipment.getTrackingUrl())
                    .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                    .build());
        }
    }
}