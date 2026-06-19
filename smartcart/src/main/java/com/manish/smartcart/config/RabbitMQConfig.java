package com.manish.smartcart.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for order processing.
 * Configures the main order queues, exchanges, and a Dead Letter Queue (DLQ)
 * architecture for safely handling message processing failures.
 */
@Configuration
public class RabbitMQConfig {

    // --- Main Queue Constants ---
    public static final String QUEUE_ORDER_PAID         = "queue.order.paid";
    public static final String EXCHANGE_ORDER           = "exchange.order";
    public static final String ROUTING_KEY_ORDER_PAID   = "routing.key.order.paid";

    // --- Dead Letter Queue (DLQ) Constants ---
    public static final String QUEUE_ORDER_PAID_DLQ     = "queue.order.paid.dlq";
    public static final String EXCHANGE_ORDER_DLQ       = "exchange.order.dlq";

    // ─── Flash Sale Marketing Constants ─────────────────────────────────────────
    public static final String EXCHANGE_MARKETING            = "exchange.marketing";
    public static final String QUEUE_FLASH_SALE_INVITE       = "queue.flash.sale.invite";
    public static final String QUEUE_FLASH_SALE_INVITE_DLQ   = "queue.flash.sale.invite.dlq";
    public static final String ROUTING_KEY_FLASH_SALE        = "routing.key.flash.sale.created";

    // ─── Per-Seller Individual Email Queue ──────────────────────────────────────
    // CONCEPT: The Orchestrator fans out ONE message per seller here.
    // Workers pick them up in parallel (concurrency = "5-10").
    // If seller #30,001's email fails → ONLY that message → DLQ.
    // The other 49,999 sellers continue unaffected. Perfect failure isolation.
    public static final String QUEUE_SELLER_EMAIL_INDIVIDUAL     = "queue.seller.email.individual";
    public static final String QUEUE_SELLER_EMAIL_INDIVIDUAL_DLQ = "queue.seller.email.individual.dlq";
    public static final String ROUTING_KEY_SELLER_EMAIL          = "routing.key.seller.email.individual";


    /* ==========================================================================
     * DEAD LETTER EXCHANGE & QUEUES (Shared Infrastructure)
     * ========================================================================== */

    /**
     * Dead Letter Exchange (DLX) for routing failed order messages.
     */
    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(EXCHANGE_ORDER_DLQ);
    }

    /**
     * Dead Letter Queue (DLQ) to persist failed messages (e.g., max retries exceeded)
     * for later operational inspection and replay.
     */
    @Bean
    public Queue orderPaidDeadLetterQueue() {
        return new Queue(QUEUE_ORDER_PAID_DLQ, true);
    }

    /**
     * Dead Letter Queue for failed flash sale invitation emails.
     * Ensures un-sendable marketing messages are retained for operational review.
     */
    @Bean
    public Queue flashSaleInviteDlq(){
        return new Queue(QUEUE_FLASH_SALE_INVITE_DLQ, true);
    }

    /**
     * Binds the order DLQ to the shared DLX.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(orderPaidDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ROUTING_KEY_ORDER_PAID);
    }

    /**
     * Binds the marketing DLQ to the shared DLX.
     */
    @Bean
    public Binding flashSaleInviteDlqBinding(){
        return BindingBuilder.bind(flashSaleInviteDlq())
                .to(orderDeadLetterExchange())
                .with(ROUTING_KEY_FLASH_SALE);
    }

    /* ==========================================================================
     * ORDER PROCESSING MAIN QUEUES
     * ========================================================================== */

    /**
     * Main exchange for order-related domain events.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    /**
     * Main queue for processing paid orders.
     * Configured to route rejected or expired messages to the shared DLX.
     */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_PAID)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLQ)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_ORDER_PAID)
                .build();
    }

    /**
     * Binds the main order queue to the order exchange.
     */
    @Bean
    public Binding binding(Queue orderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_ORDER_PAID);
    }


    /* ==========================================================================
     * MARKETING / FLASH SALE MAIN QUEUES
     * ========================================================================== */

    /**
     * Exchange for marketing campaigns and flash sale notifications.
     */
    @Bean
    public TopicExchange marketingExchange() {
        return new TopicExchange(EXCHANGE_MARKETING);
    }

    /**
     * Main queue for dispatching flash sale invitations to users.
     * Configured to route delivery failures to the shared DLX.
     */
    @Bean
    public Queue flashSaleInviteQueue(){
        return QueueBuilder.durable(QUEUE_FLASH_SALE_INVITE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLQ)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_FLASH_SALE)
                .build();
    }

    /**
     * Binds the main marketing queue to the marketing exchange.
     */
    @Bean
    public Binding flashSaleInviteBinding() {
        return BindingBuilder.bind(flashSaleInviteQueue())
                .to(marketingExchange())
                .with(ROUTING_KEY_FLASH_SALE);
    }

    /* ==========================================================================
     * PER-SELLER INDIVIDUAL EMAIL QUEUES (Fan-Out Architecture)
     * ========================================================================== */

    /**
     * Dead Letter Queue for failed atomic seller email dispatches.
     * Isolates individual SMTP delivery failures from the broader campaign execution,
     * ensuring that one failed delivery does not impact the remaining batch.
     */
    @Bean
    public Queue sellerEmailIndividualDlq(){
        return new Queue(QUEUE_SELLER_EMAIL_INDIVIDUAL_DLQ, true);
    }

    /**
     * Binds the individual seller email DLQ to the shared DLX.
     */
    @Bean
    public Binding sellerEmailIndividualDlqBinding(){
        return BindingBuilder.bind(sellerEmailIndividualDlq())
                .to(orderDeadLetterExchange()) // Re-use the existing shared DLX
                .with(ROUTING_KEY_SELLER_EMAIL);
    }

    /**
     * Main queue for executing atomic per-seller email dispatches.
     * Populated by the upstream Orchestrator and consumed concurrently by worker nodes.
     * Configured to route individually rejected messages to the shared DLX.
     */
    @Bean
    public Queue sellerEmailIndividualQueue(){
        return QueueBuilder.durable(QUEUE_SELLER_EMAIL_INDIVIDUAL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLQ)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_SELLER_EMAIL)
                .build();
    }

    /**
     * Binds the main per-seller email queue to the marketing exchange.
     */
    @Bean
    public Binding sellerEmailIndividualBinding() {
        return BindingBuilder.bind(sellerEmailIndividualQueue())
                .to(marketingExchange())
                .with(ROUTING_KEY_SELLER_EMAIL);
    }
    /* ==========================================================================
     * GLOBAL MESSAGE CONVERTERS
     * ========================================================================== */

    /**
     * Global message converter to serialize Java Objects to JSON payloads.
     */
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
