package com.manish.smartcart.service.notifications;

import com.manish.smartcart.config.RabbitMQConfig;
import com.manish.smartcart.dto.event.SellerInviteEmailEvent;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * ─── ROLE: WORKER ────────────────────────────────────────────────────────────
 * Sends ONE email per RabbitMQ message invocation.
 * Runs with concurrency = "5-10" meaning Spring AMQP starts 5 to 10 threads,
 * each consuming independently. 5-10 emails sent in parallel at all times.

 * ─── WHY concurrency = "5-10"? ───────────────────────────────────────────────
 * "5-10" means: start with 5 consumer threads, scale up to 10 under load.
 * Each thread picks a message, sends an email, ACKs, picks next message.
 * At 10 parallel threads × ~1.5s per email = ~6-7 emails/second.
 * 50,000 emails ÷ 7/sec ≈ 2 hours. Acceptable for a marketing campaign.
 * In production, you'd increase this or run multiple app instances.

 * ─── FAILURE ISOLATION ───────────────────────────────────────────────────────
 * If Gmail rejects seller #30,001's email:
 *   → That one message is NACKed → goes to queue.seller.email.individual.dlq
 *   → The other 49,999 messages continue processing normally.
 * Compare with the old for-loop design: one exception would stop ALL emails.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerEmailWorker {

    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    /**
     * Consumes an atomic email instruction and executes a synchronous SMTP transmission.
     * * @param event The individualized email routing payload.
     * @throws RuntimeException if the SMTP server rejects the payload or times out,
     * triggering a NACK and subsequent DLQ routing.
     */
    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_SELLER_EMAIL_INDIVIDUAL,
            concurrency = "5-10" // 5 min threads, scales to 10 under message load
    )
    public void sendInviteEmail(SellerInviteEmailEvent event){
        try{
            String htmlBody = emailTemplateBuilder.buildFlashSaleInviteEmail(
                    event.getSellerName(),
                    event.getEventName(),
                    event.getStartTime(),
                    event.getEndTime()
            );

            // sendMail is already @Async in EmailService — but here we're already
            // in a background thread (RabbitMQ consumer thread), so @Async is a
            // no-op. The call is effectively synchronous within this worker thread.
            emailService.sendMail(
                    event.getSellerEmail(),
                    "Action Required: 🔥You're Invited: Submit to " + event.getEventName() + "!",
                    htmlBody,
                    "CognitoCart Marketing"
            );
            log.debug("✅ [Worker] Dispatch successful for user: {}", event.getSellerEmail());
        }catch (Exception e){
            log.error("❌ [Worker] SMTP dispatch failed for user: {}. Routing to DLQ.", event.getSellerEmail(), e);

            // Rethrowing forces the RabbitMQ consumer to NACK the message.
            // Throwing here tells RabbitMQ: "I could not process this message."
            // RabbitMQ will NACK it → route to queue.seller.email.individual.dlq.
            // ONLY this seller's message is affected. All others continue normally.
            throw new RuntimeException("Synchronous SMTP delivery failed for: " + event.getSellerEmail(), e);
        }
    }
}
