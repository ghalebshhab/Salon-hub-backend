package com.salon.backend.DTOs.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Booking as shown in lists. Live fields ({@code journeyStatus} to
 * {@code completedAt}) never include the customer's coordinates; those are only
 * in the tracking response for the assigned employee and the owner.
 */
public record BookingResponse(
        Long id,
        Long salonId,
        String salonName,
        String salonLocation,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhoneNumber,
        String customerLocation,
        LocalDate bookingDate,
        LocalTime bookingTime,
        BookingStatus status,
        List<BookingServiceItemResponse> services,
        BigDecimal totalPrice,
        Integer totalDurationMinutes,
        Long assignedEmployeeId,
        String assignedEmployeeName,
        String assignedEmployeeSpecialty,
        String note,
        String ownerNote,
        List<BookingRescheduleProposalResponse> rescheduleProposals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        JourneyStatus journeyStatus,
        Integer etaMinutes,
        LocalDateTime arrivedAt,
        LocalDateTime serviceStartedAt,
        LocalDateTime expectedEndAt,
        LocalDateTime completedAt,
        Double salonLatitude,
        Double salonLongitude
) {
}
