package com.manish.smartcart.service.notifications;

import com.manish.smartcart.config.RabbitMQConfig;
import com.manish.smartcart.dto.event.FlashSaleCreatedEvent;
import com.manish.smartcart.dto.event.SellerEmailProjection;
import com.manish.smartcart.dto.event.SellerInviteEmailEvent;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * ─── ROLE: ORCHESTRATOR ─────────────────────────────────────────────────────
 * This class does ONE job: receive 1 event and fan it out to 50,000 messages.
 * It does NOT send emails. That is the Worker's job.

 * ─── WHY THE SPLIT? ──────────────────────────────────────────────────────────
 * BOTTLENECK 1 — OOM:
 *   The old design used findByRole(SELLER) which loads ALL sellers at once.
 *   FIX: We use a projection (2 columns only) + pagination (500 at a time).
 *   JVM never holds more than 500 lightweight objects at once.

 * BOTTLENECK 2 — Broker Timeout:
 *   Sending 50,000 SMTP emails takes 13+ hours in one thread.
 *   RabbitMQ's consumer_timeout would cut the connection → NACK → DLQ → retry
 *   → infinite loop of duplicate emails.
 *   FIX: This Orchestrator just PUBLISHES 50,000 tiny messages (takes seconds).
 *   The broker gets its ACK in seconds, not hours.
 *   The actual email sending is done by SellerEmailWorker in parallel.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleNotificationListener {

    private final UsersRepository usersRepository;
    private final RabbitTemplate rabbitTemplate;// Used to publish, not to email!

    // DateTimeFormatter is thread-safe and stateless — safe to share as a constant.
    private static final DateTimeFormatter FORMATTER  =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // Max sellers loaded into JVM memory at one time.
    // Tune based on your server's heap size. 500 is safe for most configs.
    private static final int PAGE_SIZE = 500;

    /**
     * Consumes the global campaign creation event and fans out individual email tasks.
     *
     * @param event The scheduled platform event payload.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FLASH_SALE_INVITE)
    public void fanOutSellerInvites(FlashSaleCreatedEvent event){
        log.info("📥 [FanOut Orchestrator] Campaign '{}' received. Initiating asynchronous distribution to sellers...", event.getEventName());

        // Pre-format dates ONCE here so each Worker message has a ready-to-use string.
        // Formatting 50,000 times in 50,000 workers is wasteful.
        // Pre-format dates to eliminate redundant CPU cycles in downstream worker nodes.
        String startTime = event.getStartTime().format(FORMATTER);
        String endTime = event.getEndTime().format(FORMATTER);

        int pageNumber = 0;
        int totalFanOut = 0;

        // Page is currently used because repository returns Page.
        // If total count is not required, consider Slice to avoid COUNT(*) queries.
        Slice<SellerEmailProjection> slice;

        // ─── THE PAGINATED FAN-OUT LOOP ──────────────────────────────────────
        // Each iteration:
        //   1. Loads 500 sellers (2 columns each — lightweight projection)
        //   2. Publishes 500 tiny RabbitMQ messages (~200 bytes each)
        //   3. Those 500 projection objects become garbage-collectable
        //   4. Next iteration loads the NEXT 500 (fresh, bounded memory)
        // Loop ends when there are no more pages of sellers.

        do{
            slice = usersRepository.findByRole(Role.SELLER, PageRequest.of(pageNumber, PAGE_SIZE));

            for(SellerEmailProjection seller : slice.getContent()){
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_MARKETING,
                        RabbitMQConfig.ROUTING_KEY_SELLER_EMAIL,
                        SellerInviteEmailEvent.builder()
                                .sellerEmail(seller.getEmail())
                                .sellerName(seller.getFullName())
                                .eventName(event.getEventName())
                                .startTime(startTime)
                                .endTime(endTime)
                                .build()
                );
                totalFanOut++;
            }
            pageNumber++;
        }while(slice.hasNext());
        // ─────────────────────────────────────────────────────────────────────
        // This line is reached in SECONDS regardless of seller count.
        // RabbitMQ ACKs this message cleanly. No timeout. No infinite loop.
        log.info("✅ [FanOut Orchestrator] Distribution complete. {} atomic email jobs delegated to workers for campaign '{}'.",
                totalFanOut, event.getEventName());
    }
}
