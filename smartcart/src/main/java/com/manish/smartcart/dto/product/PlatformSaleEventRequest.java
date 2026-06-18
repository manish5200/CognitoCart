package com.manish.smartcart.dto.product;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Payload submitted by the Admin to schedule a massive platform-wide event.
 */
@Data
public class PlatformSaleEventRequest {

    @NotBlank(message = "Event name is required (e.g., Diwali Mega Sale)")
    private String eventName;

    private String description;
    @NotNull(message = "Start time is required")

    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
}
