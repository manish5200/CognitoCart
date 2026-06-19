package com.manish.smartcart.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight RabbitMQ message payload published when Admin creates a Flash Sale Event.
 * Follows the same pattern as OrderPaidEvent — lives in dto/event/ package.
 * Serializable ensures Jackson can serialize/deserialize it over the AMQP wire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleCreatedEvent {
    private Long eventId;
    private String eventName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
