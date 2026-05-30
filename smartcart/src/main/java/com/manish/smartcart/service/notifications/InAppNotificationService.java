package com.manish.smartcart.service.notifications;

import com.manish.smartcart.dto.notification.NotificationListResponse;
import com.manish.smartcart.dto.notification.NotificationResponse;
import com.manish.smartcart.enums.NotificationType;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.model.notification.Notification;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.NotificationRepository;
import com.manish.smartcart.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String message){
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found" + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("In-App Notification created for User ID {}: {}", userId, title);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getUserNotifications(Long userId, Pageable pageable){
        int unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Page<NotificationResponse> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this :: mapToResponse);

        return NotificationListResponse.builder()
                .unreadCount(unreadCount)
                .notifications(notifications)
                .build();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found" + notificationId));

        if(!notification.getUser().getId().equals(userId)){
            throw new BusinessLogicException("Access Denied: Not your notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
