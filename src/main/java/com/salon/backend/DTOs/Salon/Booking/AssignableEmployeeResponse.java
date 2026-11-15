package com.salon.backend.DTOs.Salon.Booking;

/**
 * A salon employee who can perform every service of a booking, and whether they
 * are free for its exact time. {@code reason} is null when {@code available}.
 */
public record AssignableEmployeeResponse(
        Long id,
        String fullName,
        String specialty,
        boolean available,
        String reason
) {
}
