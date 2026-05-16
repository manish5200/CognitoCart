package com.manish.smartcart.service;


import com.manish.smartcart.config.RabbitMQConfig;
import com.manish.smartcart.dto.event.OrderPaidEvent;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.PaymentStatus;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns all business logic for Razorpay payment verification and webhook processing.
 * CONCEPT — Layered Architecture:
 * Controllers must NEVER touch repositories or fire events directly.
 * This service is the single source of truth for payment state transitions.
 * RESPONSIBILITIES:
 *   1. Verify Razorpay signature cryptographically
 *   2. Transition Order/Payment status: PENDING → PAID / FAILED
 *   3. Publish OrderPaidEvent to RabbitMQ for async invoice generation
 *   4. Idempotency: safe to call multiple times with the same input
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessingService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentService paymentService;

    /**
     * Called by the frontend after user completes payment on Razorpay UI.
     * Verifies the HMAC signature, marks the order PAID, fires the invoice event.
     * IDEMPOTENT: returns early if already PAID — safe to call twice.
     */
    @Transactional
    public Order verifyAndConfirmPayment(String razorpayOrderId,
                                         String razorpayPaymentId,
                                         String razorpaySignature){

        // 1. Cryptographic check — reject tampered requests
        boolean isValid = paymentService.verifyPaymentSignature(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if(!isValid){
            log.warn("Payment signature is INVALID for Razorpay Order: {} ", razorpayOrderId);
            throw new BusinessLogicException("Payment verification failed. Invalid signature");
        }

        // 2. Load order with items (avoids LazyInitializationException later)
        Order order = orderRepository.findByRazorpayOrderIdWithItems(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found for Razorpay ID: " + razorpayOrderId));

        // 3. Idempotency guard — already processed by webhook, skip silently
        if(order.getOrderStatus() == OrderStatus.PAID){
            log.info("Idempotency: Order {} already PAID. Skipping.", order.getId());
            return order;
        }

        // 4. State transition: PAYMENT_PENDING → PAID
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setRazorpaySignature(razorpaySignature);
        orderRepository.save(order);

        // 5. Fire async event → RabbitMQ → InvoiceConsumer builds PDF + sends email
        publishOrderPaidEvent(order.getId(), "frontend-callback");

        return order;
    }

    /**
     * Called by Razorpay's server-to-server webhook.
     * Handles: payment.captured, order.paid, payment.failed
     * IDEMPOTENT: checks current status before any mutation.
     */
    public void processRazorpayWebhook(String payload){

        JSONObject json = new JSONObject(payload);
        String eventType = json.getString("event");

        if("payment.captured".equals(eventType) || "order.paid".equals(eventType)){
            JSONObject paymentEntity = json.getJSONObject(payload)
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");

            orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order ->{
                if(order.getOrderStatus() != OrderStatus.PAID){
                    log.info("Webhook: promoting Order {} to PAID", order.getId());
                    order.setOrderStatus(OrderStatus.PAID);
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setRazorpayPaymentId(razorpayPaymentId);
                    orderRepository.save(order);
                    publishOrderPaidEvent(order.getId(), "razorpay-webhook");
                }else{
                    log.info("Webhook idempotency: Order {} already PAID.", razorpayOrderId);
                }
            });
        }else if("payment.failed".equals(eventType)){

            JSONObject paymentEntity = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
            String razorpayOrderId = paymentEntity.getString("order_id");

            orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
                if (order.getPaymentStatus() != PaymentStatus.PAID) {
                    log.warn("Webhook: payment FAILED for Order {}", order.getId());
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                }
            });
        }
    }

    // Shared helper — single place where we publish to RabbitMQ
    private void publishOrderPaidEvent(Long orderId, String source) {
        OrderPaidEvent event = new OrderPaidEvent(orderId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_ORDER,
                RabbitMQConfig.ROUTING_KEY_ORDER_PAID,
                event);
        log.info("OrderPaidEvent published for Order {} [source={}]", orderId, source);
    }
}
