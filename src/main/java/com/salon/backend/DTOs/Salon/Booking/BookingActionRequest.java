package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalTime;

public record BookingActionRequest(
        String ownerNote,
        LocalTime ownerSuggestedTime,
        Long assignedEmployeeId
) {
}
