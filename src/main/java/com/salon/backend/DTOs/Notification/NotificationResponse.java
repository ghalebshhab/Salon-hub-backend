package com.salon.backend.DTOs.Notification;

import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.salons.bookings.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record NotificationResponse(
        Long id,
        Long bookingId,
        Long salonId,
        String salonName,
        NotificationEventType eventType,
        String message,
        LocalDate bookingDate,
        LocalTime bookingTime,
        LocalDate suggestedDate,
        LocalTime suggestedTime,
        BookingStatus bookingStatus,
        String targetPath,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean read
) {
}
