package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.dto.order.ShipmentRequest;
import com.manish.smartcart.dto.order.ShipmentTrackingDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.Shipment;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ShipmentRepository;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;

    /*
     * CONCEPT: This is the core "fulfillment" operation.
     * When admin calls this:
     *   1. We check the order exists and is in a shippable state.
     *   2. We create a Shipment record linked to the Order.
     *   3. We promote the Order status to SHIPPED.
     *   4. We trigger the "Order Shipped" email — which will now include tracking info.
     *
     * @Transactional ensures both the Shipment save and the Order status update
     * happen together. If either fails, both roll back — no orphan shipments.
     */

    @Transactional
    public OrderResponse attachShipmentAndShip(Long orderId, ShipmentRequest request){
        // 1. Fetch the order — throw a clear error if it doesn't exist
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new RuntimeException("Order not found with ID: " + orderId));

        // 2. Guard: only CONFIRMED or PACKED orders can be shipped.
        //    Prevents double-shipping or shipping an unpaid order.
        if(order.getOrderStatus() != OrderStatus.CONFIRMED
        && order.getOrderStatus() != OrderStatus.PACKED){
            throw new RuntimeException(
                    "Order #" + orderId + " cannot be shipped. Current status: " + order.getOrderStatus()
                            + ". Order must be CONFIRMED or PACKED first.");
        }

        // 3. Guard: prevent duplicate shipment creation for the same order
        if(shipmentRepository.findByOrder_Id(orderId).isPresent()){
            throw new RuntimeException("A shipment already exists for Order #" + orderId);
        }

        // 4. Build and save the Shipment entity
        Shipment shipment = Shipment.builder()
                .order(order)
                .courierName(request.getCourierName())
                .trackingNumber(request.getTrackingNumber())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .dispatchedBy(request.getDispatchedBy())
                .build();


        // Auto-build tracking URL if admin didn't provide one
        // CONCEPT: You can build courier-specific URLs like Delhivery/BlueDart here
        if(request.getTrackingUrl() != null){
            shipment.setTrackingUrl(request.getTrackingUrl());
        }else{
            // Generic fallback — will be replaced with real courier URL logic later
            shipment.setTrackingUrl("https://www.google.com/search?q=" + request.getTrackingNumber());
        }

        shipmentRepository.save(shipment);
        log.info("Shipment created for Order #{} — AWB: {}", orderId, request.getTrackingNumber());

        // 5. Promote Order to SHIPPED status
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // 6. Build the response — manually inject shipment tracking into OrderResponse
        //    because OrderMapper doesn't know about Shipment (it's not on the Order entity)
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
        orderResponse.setShipmentTracking(
                ShipmentTrackingDTO.builder()
                        .courierName(shipment.getCourierName())
                        .trackingNumber(shipment.getTrackingNumber())
                        .trackingUrl(shipment.getTrackingUrl())
                        .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                        .build()
        );

        // 7. Fire the "Your order has been shipped!" email — async, non-blocking
        orderNotificationService.sendStatusUpdateEmail(orderResponse);

        return orderResponse;
    }
}
