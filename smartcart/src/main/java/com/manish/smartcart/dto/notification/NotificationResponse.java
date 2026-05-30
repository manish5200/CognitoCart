package com.manish.smartcart.dto.notification;

import com.manish.smartcart.enums.NotificationType;
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
