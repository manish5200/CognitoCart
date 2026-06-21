package com.manish.smartcart.sale.dto;

import com.manish.smartcart.shared.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformSaleEventResponse {
    private Long id;
    private String eventName;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventStatus status;
}
