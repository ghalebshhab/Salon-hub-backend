package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalTime;
import java.util.List;

public record BookingAvailabilitySlotResponse(
        LocalTime time,
        List<AvailableBookingEmployeeResponse> eligibleEmployees
) {
}
