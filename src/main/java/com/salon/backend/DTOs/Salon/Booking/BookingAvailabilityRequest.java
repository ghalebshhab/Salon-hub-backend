package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;
import java.util.List;

public record BookingAvailabilityRequest(
        Long salonId,
        List<Long> serviceIds,
        LocalDate bookingDate,
        Long employeeId
) {
}
