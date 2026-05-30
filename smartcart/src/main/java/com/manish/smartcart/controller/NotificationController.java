package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.notification.NotificationListResponse;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.notifications.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-App Notification Hub for users")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final InAppNotificationService notificationService;


    @Operation(summary = "Get User Notifications",
            description = "Fetch paginated notifications and unread count for the bell icon.")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved")
    @GetMapping
    public ResponseEntity<NotificationListResponse>getUserNotifications(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable){
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }


    @Operation(summary = "Mark specific notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok("Notification marked as read");
    }


    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails =  (CustomUserDetails) authentication.getPrincipal();
        if(userDetails == null) {
            throw new BusinessLogicException("Authentication missing. Please log in.");
        }
        return userDetails.getUser().getId();
    }
}
