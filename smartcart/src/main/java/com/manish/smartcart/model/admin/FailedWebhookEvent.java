package com.manish.smartcart.model.admin;

import com.manish.smartcart.enums.DlqStatus;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "failed_webhook_events")
public class FailedWebhookEvent extends BaseEntity {

    // Webhook JSON payloads can be huge! We strictly define this as TEXT in Postgres.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    // We must save the signature! Otherwise Razorpay validation will fail when we replay it.
    @Column(nullable = false)
    private String signature;

    @Column(name = "event_type")
    private String eventType; // e.g. "order.paid"

    // The actual Java Exception message ("NullPointerException at line 42")
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqStatus status;
}
