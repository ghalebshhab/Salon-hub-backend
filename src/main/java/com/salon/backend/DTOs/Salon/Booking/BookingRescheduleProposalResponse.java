package com.salon.backend.DTOs.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingRescheduleProposalResponse(
        Long id,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String note,
        BookingRescheduleStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
}
