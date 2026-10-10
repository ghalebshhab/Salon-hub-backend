package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateBookingRequest(
        Long salonId,
        List<Long> serviceIds,
        Long assignedEmployeeId,
        String customerPhoneNumber,
        String customerLocation,
        LocalDate bookingDate,
        LocalTime bookingTime,
        String note
) {
}
