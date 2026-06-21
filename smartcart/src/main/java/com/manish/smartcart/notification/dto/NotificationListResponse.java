package com.manish.smartcart.notification.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class NotificationListResponse {
    private int unreadCount;
    private Page<NotificationResponse> notifications;
}
