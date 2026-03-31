package com.manish.smartcart.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ─── Main Queue Constants ───────────────────────────────────────────────────
    public static final String QUEUE_ORDER_PAID         = "queue.order.paid";
    public static final String EXCHANGE_ORDER           = "exchange.order";
    public static final String ROUTING_KEY_ORDER_PAID   = "routing.key.order.paid";

    // ─── Dead Letter Queue (DLQ) Constants ─────────────────────────────────────
    // CONCEPT: A DLQ is a safety net. If a message fails processing 3 times
    // (e.g., DB is down, SMTP refuses), RabbitMQ moves it here instead of
    // silently deleting it. Operations teams can inspect and replay it later.
    // This is how Amazon SQS DLQs, Azure Service Bus and Kafka DLTs work.
    public static final String QUEUE_ORDER_PAID_DLQ     = "queue.order.paid.dlq";
    public static final String EXCHANGE_ORDER_DLQ       = "exchange.order.dlq";

    // ─── 1. Dead Letter Exchange (DLX) ──────────────────────────────────────────
    // The DLX is the router that receives dead/failed messages from the main queue.
    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(EXCHANGE_ORDER_DLQ);
    }

    // ─── 2. Dead Letter Queue ────────────────────────────────────────────────────
    // This is where failed messages land safely so they are never lost.
    @Bean
    public Queue orderPaidDeadLetterQueue() {
        return new Queue(QUEUE_ORDER_PAID_DLQ, true); // durable = survives restarts
    }

    // ─── 3. Bind DLQ to DLX ─────────────────────────────────────────────────────
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(orderPaidDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ROUTING_KEY_ORDER_PAID); // Re-use same routing key for consistency
    }

    // ─── 4. Main Queue (with DLX configured) ───────────────────────────────────
    // CONCEPT: We tell the main queue: "if you reject a message, send it to DLX."
    // x-dead-letter-exchange → which exchange gets the dead message
    // x-dead-letter-routing-key → which routing key to use on the DLX
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_PAID)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLQ)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_ORDER_PAID)
                .build();
    }

    // ─── 5. Main Exchange ────────────────────────────────────────────────────────
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    // ─── 6. Bind Main Queue to Main Exchange ─────────────────────────────────────
    @Bean
    public Binding binding(Queue orderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_ORDER_PAID);
    }

    // ─── 7. Message Converter ────────────────────────────────────────────────────
    // Automatically translates Java DTOs ↔ JSON for RabbitMQ messages
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
