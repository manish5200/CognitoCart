package com.manish.smartcart.dto.product;

import com.manish.smartcart.enums.EventStatus;
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
