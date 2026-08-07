package com.manish.smartcart.order.dto;

import com.manish.smartcart.shared.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Read-only projection of a single order timeline event.
 * occurredAt maps to BaseEntity.createdAt — set once at INSERT, never updated.
 */
@Data
@Builder
public class OrderEventResponse {
    private OrderStatus   status;
    private String        actor;
    private String        note;
    private LocalDateTime occurredAt; // sourced from BaseEntity.createdAt
}
