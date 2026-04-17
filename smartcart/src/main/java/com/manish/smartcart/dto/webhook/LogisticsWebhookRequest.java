package com.manish.smartcart.dto.webhook;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.manish.smartcart.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Inbound payload sent by logistics carriers (BlueDart, Delhivery, FedEx)
 * when a shipment status changes at their end.

 * CONCEPT — Webhook vs REST API:
 * Normal REST: YOU call THEM (you ask for data).
 * Webhook: THEY call YOU (they push data to you automatically).

 * This DTO is the inbound shape of what a carrier POSTs to:
 * POST /api/v1/webhooks/logistics
 */

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogisticsWebhookRequest {

    /**
     * AWB (Air Waybill) — the unique package tracking ID assigned by the courier.
     * This is the correlation key: the only identifier the carrier knows about our shipment.
     * Example: "BLD123456789IN", "DTDC987654321"
     */
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    /**
     * New shipment status reported by the carrier.
     * Maps to our ShipmentStatus enum: OUT_FOR_DELIVERY, DELIVERED, FAILED, IN_TRANSIT etc.
     */
    @NotNull(message = "Shipment status is required")
    private ShipmentStatus status;

    /**
     * Name of the carrier pushing this update — stored for audit trail.
     * Example: "BlueDart", "Delhivery", "Amazon Logistics"
     */
    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    /**
     * Timestamp when the carrier recorded this event at their end.
     * @JsonFormat accepts "yyyy-MM-dd HH:mm:ss" string format in the JSON body.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTimeStamp;

    /**
     * Optional human-readable note from the carrier.
     * Example: "Delivered to neighbor", "Door locked — left at reception"
     */
    private String remarks;
}
