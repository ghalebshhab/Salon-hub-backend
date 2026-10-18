package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;
import java.util.List;

public record BookingAvailabilityResponse(
        Long salonId,
        LocalDate bookingDate,
        int totalDurationMinutes,
        int slotIntervalMinutes,
        int minimumLeadMinutes,
        List<BookingAvailabilitySlotResponse> slots
) {
}
