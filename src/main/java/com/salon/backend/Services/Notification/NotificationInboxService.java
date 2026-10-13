package com.salon.backend.Services.Notification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Notification.NotificationPageResponse;
import com.salon.backend.DTOs.Notification.NotificationResponse;
import com.salon.backend.DTOs.Notification.UnreadNotificationCountResponse;

public interface NotificationInboxService {

    ApiResponse<NotificationPageResponse> getNotifications(Long recipientId, Integer page, Integer size);

    ApiResponse<UnreadNotificationCountResponse> getUnreadCount(Long recipientId);

    ApiResponse<NotificationResponse> markAsRead(Long notificationId, Long recipientId);

    ApiResponse<UnreadNotificationCountResponse> markAllAsRead(Long recipientId);
}
