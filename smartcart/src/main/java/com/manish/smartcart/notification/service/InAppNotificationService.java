package com.manish.smartcart.notification.service;

import com.manish.smartcart.notification.dto.NotificationListResponse;
import com.manish.smartcart.notification.dto.NotificationResponse;
import com.manish.smartcart.shared.enums.NotificationType;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.notification.model.Notification;
import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.notification.repository.NotificationRepository;
import com.manish.smartcart.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public void createNotification(java.util.UUID userPublicId, NotificationType type, String title, String message){
        Users user = usersRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found" + userPublicId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("In-App Notification created for User ID {}: {}", userPublicId, title);
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
    public void markAsRead(Long userId,  UUID notificationPublicId){
        Long notificationId = notificationRepository.findByPublicId(notificationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification Public ID not found" + notificationPublicId))
                .getId();
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
                .notificationPublicId(notification.getPublicId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
