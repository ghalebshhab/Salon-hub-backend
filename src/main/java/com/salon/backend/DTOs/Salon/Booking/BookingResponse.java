package com.salon.backend.DTOs.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
        LocalDateTime updatedAt
) {
}
