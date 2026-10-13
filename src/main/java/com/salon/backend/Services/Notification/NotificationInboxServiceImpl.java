package com.salon.backend.Services.Notification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Notification.NotificationPageResponse;
import com.salon.backend.DTOs.Notification.NotificationResponse;
import com.salon.backend.DTOs.Notification.UnreadNotificationCountResponse;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationInboxServiceImpl implements NotificationInboxService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserNotificationRepo notificationRepo;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<NotificationPageResponse> getNotifications(
            Long recipientId,
            Integer page,
            Integer size) {
        if (recipientId == null) {
            return ApiResponse.error("Authenticated user is required");
        }

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) {
            return ApiResponse.error("Notification page cannot be negative");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ApiResponse.error("Notification page size must be between 1 and 100");
        }

        Page<UserNotification> notifications = notificationRepo.findByRecipientId(
                recipientId,
                PageRequest.of(
                        pageNumber,
                        pageSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                )
        );

        return ApiResponse.success(
                "Notifications fetched successfully",
                new NotificationPageResponse(
                        notifications.getContent().stream().map(this::mapToResponse).toList(),
                        notifications.getNumber(),
                        notifications.getSize(),
                        notifications.getTotalElements(),
                        notifications.getTotalPages(),
                        notifications.isFirst(),
                        notifications.isLast()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UnreadNotificationCountResponse> getUnreadCount(Long recipientId) {
        if (recipientId == null) {
            return ApiResponse.error("Authenticated user is required");
        }
        return ApiResponse.success(
                "Unread notification count fetched successfully",
                new UnreadNotificationCountResponse(
                        notificationRepo.countByRecipientIdAndReadAtIsNull(recipientId)
                )
        );
    }

    @Override
    @Transactional
    public ApiResponse<NotificationResponse> markAsRead(Long notificationId, Long recipientId) {
        if (notificationId == null || recipientId == null) {
            return ApiResponse.error("Notification owned by you was not found");
        }

        UserNotification notification = notificationRepo.findOwnedByIdForUpdate(
                notificationId,
                recipientId
        ).orElse(null);
        if (notification == null) {
            return ApiResponse.error("Notification owned by you was not found");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepo.save(notification);
        }

        return ApiResponse.success("Notification marked as read", mapToResponse(notification));
    }

    @Override
    @Transactional
    public ApiResponse<UnreadNotificationCountResponse> markAllAsRead(Long recipientId) {
        if (recipientId == null) {
            return ApiResponse.error("Authenticated user is required");
        }

        notificationRepo.markAllRead(recipientId, LocalDateTime.now());
        return ApiResponse.success(
                "All notifications marked as read",
                new UnreadNotificationCountResponse(0)
        );
    }

    private NotificationResponse mapToResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getBooking().getId(),
                notification.getSalonId(),
                notification.getSalonName(),
                notification.getEventType(),
                notification.getMessage(),
                notification.getBookingDate(),
                notification.getBookingTime(),
                notification.getSuggestedTime(),
                notification.getBookingStatus(),
                notification.getTargetPath(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getReadAt() != null
        );
    }
}
