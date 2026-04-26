package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.dto.order.ShipmentRequest;
import com.manish.smartcart.dto.order.ShipmentTrackingDTO;
import com.manish.smartcart.dto.webhook.LogisticsWebhookRequest;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.ShipmentStatus;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.Shipment;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ShipmentRepository;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public OrderResponse attachShipmentAndShip(Long orderId, ShipmentRequest request) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // CARRIER WEBHOOK: PROCESS REAL-TIME LOGISTICS STATUS UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Processes a real-time status push from a logistics carrier.

     * FLOW:
     *   Carrier pushes AWB + new status →
     *   Find our Shipment by AWB →
     *   Translate ShipmentStatus → OrderStatus →
     *   Save updated Order →
     *   if DELIVERED → send delivery + "Rate your purchase" email
     *   else         → send standard status update email

     * IDEMPOTENCY GUARD:
     * Carriers retry webhooks when they don't get a 200 fast enough.
     * We check if the order is already in the target status and return early if so.
     * Processing the same event N times has zero side effects.

     * TERMINAL STATE GUARD:
     * CANCELLED and REFUNDED orders cannot be modified by any external event.
     * A carrier marking a canceled order as delivered must not corrupt our data.
     */
    @Transactional
    public void processLogisticsUpdate(@Valid LogisticsWebhookRequest request) {
        // 1. Find shipment by AWB — JOIN FETCH loads Order + User in one SQL query
        Shipment shipment = shipmentRepository
                .findByTrackingNumberWithOrderAndUser(request.getTrackingNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No shipment found for tracking number: " + request.getTrackingNumber()));

        Order order = shipment.getOrder();

        // 2. Translate carrier status → our internal order status
        //    CONCEPT — Anti-Corruption Layer (DDD):
        //    Carrier vocabulary is isolated here. Rest of the app never sees ShipmentStatus.
        OrderStatus newOrderStatus = mapShipmentStatusToOrderStatus(request.getStatus());

        // 3. IDEMPOTENCY: already in target status — skip silently
        if(order.getOrderStatus() == newOrderStatus){
            log.warn("Idempotency skip: Order #{} already {}. Carrier: {}, AWB: {}",
                    order.getId(), newOrderStatus,
                    request.getCarrierName(), request.getTrackingNumber());
            return;
        }

        // 4. TERMINAL STATE GUARD: permanently closed orders cannot be changed
        if(isTerminalState(order.getOrderStatus())){
            log.warn("Rejected carrier update for terminal Order #{}. Current: {}, Attempted: {} | Carrier: {}",
                    order.getId(), order.getOrderStatus(), newOrderStatus, request.getCarrierName());
            return;
        }

        // 5. Commit the status transition
        OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(newOrderStatus);
        orderRepository.save(order);

        log.info("Order #{} updated: {} → {} | Carrier: {} | AWB: {} | Remarks: {}",
                order.getId(), previousStatus, newOrderStatus,
                request.getCarrierName(), request.getTrackingNumber(),
                request.getRemarks() != null ? request.getRemarks() : "none");

        // 6. Build response DTO for notification
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);


        // 7. Trigger the right email
        //    DELIVERED → special email with "Rate your purchase" CTA
        //    Anything else → standard status update email
        if (newOrderStatus == OrderStatus.DELIVERED) {
            orderNotificationService.sendDeliveryConfirmationEmail(orderResponse);
        } else {
            orderNotificationService.sendStatusUpdateEmail(orderResponse);
        }

    }

    /**
     * Translates ShipmentStatus (carrier's world) → OrderStatus (our business world).

     * CARRIER STATUS      → ORDER STATUS
     * OUT_FOR_DELIVERY    → OUT_FOR_DELIVERY  (on the way to customer)
     * DELIVERED           → DELIVERED         (job done)
     * RETURNED            → RETURN_REQUESTED  (carrier sent it back to warehouse)
     * FAILED/IN_TRANSIT   → SHIPPED           (still in transit, no order status change needed)
     */
    private OrderStatus mapShipmentStatusToOrderStatus(ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus){
            case OUT_FOR_DELIVERY ->  OrderStatus.OUT_FOR_DELIVERY;
            case DELIVERED ->  OrderStatus.DELIVERED;
            case RETURNED ->   OrderStatus.RETURNED;
            default -> OrderStatus.SHIPPED;

        };
    }

    /**
     * Returns true if the order is permanently closed and must never change again.
     * CANCELLED = admin/customer closed it. REFUNDED = money returned. RETURNED = item back.
     * Note: DELIVERED is NOT terminal here — it is the final carrier event we want.
     */
    private boolean isTerminalState(OrderStatus status) {
        return status == OrderStatus.CANCELLED
                || status == OrderStatus.REFUNDED
                || status == OrderStatus.RETURNED;
    }
}
