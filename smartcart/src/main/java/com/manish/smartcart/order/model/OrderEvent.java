package com.manish.smartcart.order.model;

import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Immutable audit record of a single order status transition.
 * <p>
 * Extends BaseEntity to align with the application's 3-ID system:
 *   - id        → internal Long PK (sequence, never exposed)
 *   - publicId  → UUID for any future "get single event" API
 *   - createdAt → IS the event timestamp (set once by @CreatedDate, never updated)
 * <p>
 * NOTE: @Version (optimistic lock) from BaseEntity is harmless here — events are
 * never updated, so version will always remain 0 on every row.
 */
@Entity
@Table(name = "order_events",
        indexes = @Index(name = "idx_order_events_order_id", columnList = "order_id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SequenceGenerator(name = "entity_seq", sequenceName = "order_event_seq", allocationSize = 50)
public class OrderEvent extends BaseEntity{

    /**
     * The parent order. LAZY — we never need full Order data when querying a timeline.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id",  nullable = false)
    private Order order;

    /**
     * The status this event captures.
     * Combined with BaseEntity.createdAt, this forms one timeline entry.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus status;

    /**
     * Who triggered this transition. Structured for log filtering without a JOIN.
     * Format: "SYSTEM" | "SELLER:42" | "CUSTOMER:7" | "ADMIN:1" | "CARRIER:BlueDart"
     */
    @Column(nullable = false, length = 100)
    private String actor;

    /**
     * Optional business context. Stored as TEXT to handle long tracking notes.
     * Examples: "Razorpay: pay_xxx", "AWB: BDL123456", "Supplier XYZ restock"
     */
    @Column(columnDefinition = "TEXT")
    private String note;
}
