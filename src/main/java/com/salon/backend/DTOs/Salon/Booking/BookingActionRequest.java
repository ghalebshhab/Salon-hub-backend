package com.salon.backend.DTOs.Salon.Booking;

public record BookingActionRequest(
        String ownerNote,
        Long assignedEmployeeId
) {
}
