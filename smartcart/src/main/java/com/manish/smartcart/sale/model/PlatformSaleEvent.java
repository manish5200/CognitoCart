package com.manish.smartcart.sale.model;

import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Represents a massive, platform-wide marketing event created by the Admin.
 * Sellers "opt-in" to this event by submitting FlashSaleItems.
 */

@Entity
@Table(name = "platform_sale_events", indexes = {
        // Highly optimized composite index for ShedLock to instantly find events needing activation/deactivation
        @Index(name = "idx_event_status_times", columnList = "status, start_time, end_time")
})
@SequenceGenerator(name = "entity_seq", sequenceName = "sale_event_seq", allocationSize = 50)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PlatformSaleEvent extends BaseEntity {

    @Column(name = "event_code", unique = true, nullable = false, length = 33)
    private String eventCode;

    @Column(nullable = false, unique = true, length = 150)
    private String eventName; // Marketing identifier (e.g., "Diwali Mega Sale 2026")

    @Column(length = 500)
    private String description; // Internal description or rules for the sale

    @Column(nullable = false)
    private LocalDateTime startTime; // Exact millisecond the sale activates

    @Column(nullable = false)
    private LocalDateTime endTime; // Exact millisecond the sale terminates

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.SCHEDULED; // Initial state upon creation

    @PrePersist
    private void humanIDGenerator(){
        if(this.eventCode == null){
            this.eventCode = HumanIdGenerator.generate("EVT");
        }
    }
}
