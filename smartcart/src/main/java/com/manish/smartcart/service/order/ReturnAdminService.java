package com.manish.smartcart.service.order;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.PaymentStatus;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import com.manish.smartcart.model.product.ProductVariant;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductVariantRepository;
import com.manish.smartcart.service.RazorpayRefundService;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SRP: Responsible ONLY for admin decisions on return/replacement/exchange requests.
 * Reason to change: admin approval rules, refund policy, replacement stock logic.

 * Does NOT handle customer-facing return submission (→ ReturnService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnAdminService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;
    private final RazorpayRefundService razorpayRefundService;


    // ─── APPROVE RETURN → restore stock + issue refund ───────────────────────
    @Transactional
    public OrderResponse approveReturn(Long orderId){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessLogicException(
                    "Order is not in RETURN_REQUESTED state. Current: " + order.getOrderStatus());
        }

        // Restore stock to warehouse — stock lives on the variant, not the product
        for (OrderItem item : order.getOrderItems()) {
            // Guard: variant may be null if it was hard-deleted after the order was placed.
            // Physical stock is already gone — nothing to restore, skip safely.
            if (item.getVariant() == null) {
                log.warn("Skipping stock restore for order item on approved return {}: variant was deleted.", orderId);
                continue;
            }
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        order.setOrderStatus(OrderStatus.RETURNED);

        // Issue Razorpay refund if order was paid
        if(order.getPaymentStatus() == PaymentStatus.PAID && order.getRazorpayPaymentId() != null){
            try{
                String refundId = razorpayRefundService.initiateFullRefund(
                        order.getRazorpayPaymentId(), order.getTotalAmount());
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setOrderStatus(OrderStatus.REFUNDED);
                Order saved = orderRepository.save(order);
                orderNotificationService.sendRefundEmail(orderMapper.toOrderResponse(saved), refundId);
                log.info("Return approved and refund issued — orderId={} refundId={}", orderId, refundId);
                return orderMapper.toOrderResponse(saved);

            }catch (Exception e){
                // Stock already restored — save RETURNED status then surface the error
                orderRepository.save(order);
                throw new BusinessLogicException(
                        "Stock restored and order marked RETURNED. Razorpay refund failed: " +
                                e.getMessage() + " — process manually via Razorpay dashboard.");
            }
        }
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Approve REPLACEMENT or EXCHANGE → re-check stock → re-ship
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse approveReplacement(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if(order.getOrderStatus() != OrderStatus.REPLACEMENT_REQUESTED &&
                order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED) {
            throw new BusinessLogicException(
                    "Order must be in REPLACEMENT_REQUESTED or EXCHANGE_REQUESTED state. " +
                            "Current: " + order.getOrderStatus());
        }

        // Re-check stock at approval time — may have dropped since customer's request
        for(OrderItem item : order.getOrderItems()) {

            // Guard: variant null means it was hard-deleted — cannot ship a replacement
            if (item.getVariant() == null) {
                throw new BusinessLogicException(
                        "Cannot approve replacement — the original variant no longer exists. " +
                                "Approve a RETURN (refund) instead.");
            }

            // Re-fetch fresh from DB — item.getVariant() may have stale cache values
            ProductVariant freshVariant = productVariantRepository.findById(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant no longer exists. Approve a RETURN (refund) instead."));

            int available = freshVariant.getAvailableStock(); // stockQuantity - reservedQuantity
            if(available < item.getQuantity()) {
                throw new BusinessLogicException(
                        "Cannot approve replacement — insufficient stock for: " +
                                freshVariant.getDisplayLabel() +
                                " (SKU: " + freshVariant.getSku() + ")" +
                                ". Available: " + available +
                                ". Consider approving a RETURN (refund) instead.");
            }

            freshVariant.setStockQuantity(freshVariant.getStockQuantity() - item.getQuantity());
            productVariantRepository.save(freshVariant);
        }
        order.setOrderStatus(OrderStatus.REPLACEMENT_SHIPPED);
        Order saved = orderRepository.save(order);

        // Admin then attaches the new shipment tracking via
        // existing POST /api/v1/admin/{orderId}/shipment endpoint
        orderNotificationService.sendStatusUpdateEmail(orderMapper.toOrderResponse(saved));
        log.info("Replacement approved and stock decremented for Order ID: {}", orderId);
        return orderMapper.toOrderResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Reject RETURN/REPLACEMENT/EXCHANGE
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse rejectReturn(Long orderId, String adminComment){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if(order.getOrderStatus() != OrderStatus.RETURN_REQUESTED &&
                order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED &&
                order.getOrderStatus() != OrderStatus.REPLACEMENT_REQUESTED) {
            throw new BusinessLogicException(
                    "Order is not in a return-requested state. Current: " + order.getOrderStatus());
        }

        // Build and dispatch email before resetting fields, so order details map correctly
        // Send rejection email BEFORE resetting fields — mapper needs the current state
        OrderResponse responseBeforeReset = orderMapper.toOrderResponse(order);
        orderNotificationService.sendReturnRejectedEmail(responseBeforeReset, adminComment);

        // Reset order state back to delivered state, freeing them to submit a new request if needed
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setReturnRequestType(null);
        order.setReturnReason(null);
        order.setReturnDescription(null);
        order.setReturnRequestedAt(null);

        Order saved = orderRepository.save(order);
        log.info("Return request for Order ID: {} has been rejected by admin. Reason: {}", orderId, adminComment);
        return orderMapper.toOrderResponse(saved);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Get Pending Return/Replacement/Exchange requests
    // ─────────────────────────────────────────────────────────────────────────
    public List<OrderResponse> getPendingReturnRequests(){
        List<OrderStatus>pendingStatuses = List.of(
                OrderStatus.RETURN_REQUESTED,
                OrderStatus.REPLACEMENT_REQUESTED,
                OrderStatus.EXCHANGE_REQUESTED
        );
        return orderRepository.findByOrderStatusInWithItems(pendingStatuses)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }
}
