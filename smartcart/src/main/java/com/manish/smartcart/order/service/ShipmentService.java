package com.manish.smartcart.order.service;

import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.order.dto.ShipmentRequest;
import com.manish.smartcart.order.dto.ShipmentTrackingDTO;
import com.manish.smartcart.payment.dto.LogisticsWebhookRequest;
import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.enums.ShipmentStatus;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.shared.mapper.OrderMapper;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.Shipment;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.repository.ShipmentRepository;
import com.manish.smartcart.notification.service.OrderNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;
    private final OrderEventService orderEventService;
    private final DelhiveryShipmentService delhiveryShipmentService;

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
    public OrderResponse attachShipmentAndShip(UUID orderPublicId, ShipmentRequest request) {
        // 1. Fetch the order — throw a clear error if it doesn't exist
        Order order = orderRepository.findByPublicId(orderPublicId)
                .orElseThrow(()->new ResourceNotFoundException("Order not found with ID: " + orderPublicId));

        // 2. Guard: only CONFIRMED or PACKED orders can be shipped.
        //    Prevents double-shipping or shipping an unpaid order.
        if(order.getOrderStatus() != OrderStatus.CONFIRMED
                && order.getOrderStatus() != OrderStatus.PACKED){
            throw new BusinessLogicException("Order #" + orderPublicId + " cannot be shipped. " +
                    "Current status: " + order.getOrderStatus() + ". Must be CONFIRMED or PACKED.");
        }

        // 3. Guard: prevent duplicate shipment creation for the same order
        if(shipmentRepository.findByOrder_Id(order.getId()).isPresent()){
            throw new BusinessLogicException("A shipment already exists for Order #" + orderPublicId);
        }

        // ─── AWB GENERATION ───────────────────────────────────────────────────────────
        // PRODUCTION PATH: Call Shiprocket API to auto-generate the AWB tracking number.
        // MANUAL OVERRIDE PATH: Admin explicitly provided a tracking number
        //  (used for walk-in drops, non-Shiprocket couriers, or Shiprocket outages).
        String awb;
        if(request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()){
            awb = request.getTrackingNumber();
            log.info("[SHIPMENT] Manual AWB override for Order#{}: {}", orderPublicId, awb);
        }else{
            // AUTO-GENERATE via Delhivery API (standard production path)
            // Internally handles serviceability, mock mode, error logging.
            awb = delhiveryShipmentService.createShipmentAndGetAwb(order, request);
        }


// Build tracking URL — custom URL takes priority (for non-Delhivery couriers).
// Otherwise, build Delhivery's standard tracking link from the AWB.
        String trackingUrl = (request.getTrackingUrl() != null && !request.getTrackingUrl().isBlank())
                ? request.getTrackingUrl()
                : "https://www.delhivery.com/track/package/" + awb;


        // 4. Build and persist the Shipment entity
        Shipment shipment = Shipment.builder()
                .order(order)
                .courierName(request.getCourierName())
                .trackingNumber(awb)
                .trackingUrl(trackingUrl)
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .dispatchedBy(request.getDispatchedBy())
                .build();

        shipmentRepository.save(shipment);
        log.info("Shipment created for Order #{} — AWB: {}", orderPublicId, request.getTrackingNumber());

        // 5. Promote Order to SHIPPED status
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
        orderEventService.record(order.getId(), OrderStatus.SHIPPED,
                "SYSTEM", "AWB: " + request.getTrackingNumber() + " | Courier: " + request.getCourierName());

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
        // Stamp delivery time — this is when the return window clock starts
        if (newOrderStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        orderRepository.save(order);

        orderEventService.record(order.getId(), newOrderStatus,
                "CARRIER:" + request.getCarrierName(),
                request.getRemarks() != null ? request.getRemarks() : null);

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
