package com.manish.smartcart.jobs;

import com.manish.smartcart.notification.service.OrderNotificationService;
import com.manish.smartcart.payment.service.RazorpayRefundService;
import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.service.OrderService;
import com.manish.smartcart.shared.enums.PaymentStatus;
import com.manish.smartcart.shared.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupScheduler{

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RazorpayRefundService razorpayRefundService;
    private final TransactionTemplate transactionTemplate;
    private final OrderNotificationService orderNotificationService;
    private final OrderMapper orderMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // JOB 1: Abandoned Cart Cleanup
    // Fires every hour. Cancels PAYMENT_PENDING orders older than 24 hours.
    // Cron expression: "0 0 * * * *" (Second Minute Hour Day Month DayOfWeek)
    // ShedLock prevents this from firing concurrently across multiple pods.
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0 * * * *" )
    @SchedulerLock(name = "orderCleanupJob", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void processStalePendingOrders(){
        log.info("[SCHEDULER] Starting abandoned cart cleanup...");
        // 1. Define the threshold (Orders older than 24 hours)
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        // 2. Fetch stale orders
        List<Order> staleOrders = orderRepository
                .findByOrderStatusAndOrderDateBeforeWithItems(OrderStatus.PAYMENT_PENDING, threshold);

        if (staleOrders.isEmpty()) {
            log.info("[SCHEDULER] No stale pending orders found.");
            return;
        }

        int success = 0, failed = 0;
        // 3. Process each order
        for(Order order:staleOrders){
            try{
                // Pass ID — not the Order object. cancelAndReleaseStock fetches
                // fresh inside its own TransactionTemplate to avoid LazyInitializationException.
                orderService.cancelAndReleaseStock(order.getId());
                success++;
            }catch (Exception e){
                log.error("[SCHEDULER] Failed to cleanup abandoned order ID {}: {}", order.getId(), e.getMessage());
                failed++;
            }
        }
        log.info("[SCHEDULER] Abandoned cart cleanup done. Success={} Failed={}", success, failed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JOB 2: Refund Retry Sweep
    // Fires every 30 minutes. Retries Razorpay refunds for orders that entered
    // MANUAL_REFUND_REQUIRED because the initial refund call failed.
    //
    // SAGA RESOLUTION: These orders are already CANCELLED and stock is restored.
    // The only remaining task is getting the money back to the customer.
    //
    // WHY 30 MINUTES: Short enough to resolve issues quickly, long enough to
    // let transient Razorpay outages recover before the next sweep.
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 */30 * * * *")
    @SchedulerLock(name = "refundRetryJob", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void retryFailedRefunds(){
        log.info("[SCHEDULER] Starting refund retry sweep...");

        // Only sweep orders stuck in this state for more than 30 minutes.
        // Avoids retrying an order that JUST entered this state mid-current-sweep.
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Order> pendingRefunds = orderRepository
                .findByOrderStatusAndOrderDateBeforeWithItems(OrderStatus.MANUAL_REFUND_REQUIRED, threshold);

        if (pendingRefunds.isEmpty()) {
            log.info("[SCHEDULER] No pending refunds to retry.");
            return;
        }

        log.warn("[SCHEDULER] Found {} orders requiring manual refund. Attempting retry...", pendingRefunds.size());
        int success = 0, failed = 0;

        for(Order order : pendingRefunds){
            // Guard: only retry if we have a payment ID to refund against.
            // MANUAL_REFUND_REQUIRED implies payment was collected, but double-check.
            if(order.getRazorpayPaymentId() == null || order.getTotalAmount() == null){
                log.error("[SCHEDULER] Order {} has MANUAL_REFUND_REQUIRED but missing paymentId or total. " +
                        "Requires manual admin inspection.", order.getId());
                failed++;
                continue;
            }

            try {
                // 🌐 Razorpay refund HTTP call — no DB connection held
                String refundId = razorpayRefundService.initiateFullRefund(
                        order.getRazorpayPaymentId(), order.getTotalAmount());

                log.info("[SCHEDULER] Refund retry SUCCESS for Order ID {}. refundId={}", order.getId(), refundId);

                // Write REFUNDED status + send email inside a clean transaction
                transactionTemplate.executeWithoutResult(status -> {
                    Order fresh = orderRepository.findById(order.getId()).orElseThrow();
                    fresh.setPaymentStatus(PaymentStatus.REFUNDED);
                    fresh.setOrderStatus(OrderStatus.REFUNDED);
                    Order saved = orderRepository.save(fresh);
                    // Notify customer — their money is on the way
                    orderNotificationService.sendRefundEmail(orderMapper.toOrderResponse(saved), refundId);
                });
                success++;
            }catch (Exception e){
                // Still failing — leave in MANUAL_REFUND_REQUIRED, next sweep will retry.
                // After multiple failures, ops team must intervene via Razorpay dashboard.
                log.error("[SCHEDULER] Refund retry FAILED for Order ID {}. Will retry next sweep. Error: {}", order.getId(), e.getMessage());
                failed++;
            }
        }
        log.info("[SCHEDULER] Refund retry sweep done. Success={} Failed={}", success, failed);
    }
}
