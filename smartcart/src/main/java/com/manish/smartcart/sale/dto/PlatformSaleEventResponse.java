package com.manish.smartcart.sale.dto;

import com.manish.smartcart.shared.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PlatformSaleEventResponse {
    private UUID saleEventPublicId;
    private String eventName;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventStatus status;
}
