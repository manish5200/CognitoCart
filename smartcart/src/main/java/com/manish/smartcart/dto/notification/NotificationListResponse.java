package com.manish.smartcart.dto.notification;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class NotificationListResponse {
    private int unreadCount;
    private Page<NotificationResponse> notifications;
}
