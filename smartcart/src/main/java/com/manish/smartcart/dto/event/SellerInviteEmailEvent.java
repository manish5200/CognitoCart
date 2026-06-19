package com.manish.smartcart.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ─── WHAT IS THIS? ───────────────────────────────────────────────────────────
 * This is the tiny message that travels through RabbitMQ for EACH seller.
 *
 * ─── LAYMAN EXPLANATION ──────────────────────────────────────────────────────
 * Think of this as a "delivery slip" for one seller's email job.
 * The Orchestrator creates 50,000 of these slips and drops them in a queue.
 * Each slip is ~200 bytes. All 50,000 together = ~10MB in the broker.
 * Worker threads pick up one slip at a time and send one email.
 *
 * WHY Serializable?
 * When this object travels over RabbitMQ, Jackson converts it to JSON bytes.
 * Implementing Serializable is a safety contract that says:
 * "This object is safe to convert to bytes and back."
 *
 * WHY are startTime/endTime Strings here (not LocalDateTime)?
 * Formatting (LocalDateTime → "Jun 19, 2026 10:00") is done ONCE in the
 * Orchestrator before publishing. The Worker never has to format anything.
 * Simpler workers = fewer bugs.
 */

/**
 * Atomic message payload representing a single email dispatch task within the marketing fan-out architecture.
 * Designed as a lightweight data transfer object (DTO) for high-throughput AMQP message brokers.
 * <p>
 * By granulating bulk operations into these individual, serializable instructions, the system allows
 * downstream worker nodes to process heavy I/O tasks (SMTP delivery) concurrently and horizontally.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerInviteEmailEvent implements Serializable {
    private String sellerEmail;
    private String sellerName;
    private String eventName;
    private String startTime; // Pre-formatted: e.g. "Jun 25, 2026 10:00"
    private String endTime;
}
