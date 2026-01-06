package com.manish.smartcart.scheduler;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component

public class OrderCleanupScheduler{

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    public OrderCleanupScheduler(OrderRepository orderRepository,
                                 OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Runs every hour to clean up orders payment_pending for more than 24 hours.
     * Cron expression: "0 0 * * * *" (Second Minute Hour Day Month DayOfWeek)
     */
    @Scheduled(cron = "0 0 * * * *" )
    public void processStalePendingOrders(){
        log.info("Starting scheduled cleanup of stale pending orders...");

        // 1. Define the threshold (Orders older than 24 hours)
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        // 2. Fetch stale orders
        List<Order> staleOrders = orderRepository.findByOrderStatusAndOrderDateBefore(
                        OrderStatus.PAYMENT_PENDING, threshold);

        if (staleOrders.isEmpty()) {
            log.info("No stale pending orders found.");
            return;
        }

        // 3. Process each order
        for(Order order:staleOrders){
            try{
                orderService.cancelAndReleaseStock(order);
            }catch (Exception e){
                log.error("Failed to cleanup order ID {}: {}", order.getId(), e.getMessage());
            }

        }
        log.info("Completed cleanup of {} stale orders.", staleOrders.size());
    }
}
