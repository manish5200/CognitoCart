package com.manish.smartcart.notification.dto;

import com.manish.smartcart.shared.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
