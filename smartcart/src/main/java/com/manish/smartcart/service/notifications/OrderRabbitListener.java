package com.manish.smartcart.service.notifications;

import com.manish.smartcart.config.RabbitMQConfig;
import com.manish.smartcart.dto.event.OrderPaidEvent;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRabbitListener {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;
    private final InvoiceService invoiceService;

    // This method is triggered the instant a message arrives in the queue!
    // @RabbitListener makes this method instantly trigger when a JSON message enters the queue.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PAID)
    @Transactional(readOnly = true)
    public void processOrderPaidEvent(OrderPaidEvent event) {
        log.info("📥 [RabbitMQ Listener] Picked up paid order ID: {}", event.getOrderId());

        try{
            // 1. Fetch Order with Items using the custom JOIN FETCH we just added (No N+1!)
            Order order = orderRepository.findByIdWithItems(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found for ID: " + event.getOrderId()));

            // 2. Map to Response (Safe because we eagerly fetched items)
            OrderResponse response = orderMapper.toOrderResponse(order);

            // 3. Generate PDF Heavy Task (Slow! 500ms)
            // Note: Use the exact invoiceService.generateInvoice(orderResponse) method from your codebase!
            byte[] invoicePdf = invoiceService.generateInvoice(response);

            // 4. Dispatch Emails Heavy Task (Very Slow! 2000ms)
            orderNotificationService.sendEmailNotification(response);
            orderNotificationService.sendInvoiceEmail(response, invoicePdf);

            log.info("✅ [RabbitMQ Listener] Emails and PDF sent successfully for Order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("❌ [RabbitMQ Listener] Failed to process Order Paid Event for Order {}", event.getOrderId(), e);
            // By throwing an exception here, we tell RabbitMQ to NACK (Negative Acknowledge) the message.
            // RabbitMQ will safely put the message back in the queue and try again later! Never lose an email!
            throw new RuntimeException(e);
        }
    }
}
