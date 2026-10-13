package com.salon.backend.Controllers.Notifications;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Notification.NotificationPageResponse;
import com.salon.backend.DTOs.Notification.NotificationResponse;
import com.salon.backend.DTOs.Notification.UnreadNotificationCountResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Notification.NotificationInboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationInboxService notificationInboxService;

    @GetMapping
    public ApiResponse<NotificationPageResponse> getNotifications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationInboxService.getNotifications(currentUser.getId(), page, size);
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationInboxService.getUnreadCount(currentUser.getId());
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationInboxService.markAsRead(notificationId, currentUser.getId());
    }

    @PutMapping("/read-all")
    public ApiResponse<UnreadNotificationCountResponse> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationInboxService.markAllAsRead(currentUser.getId());
    }
}
