package com.salon.backend.DTOs.Salon.Employee;

import java.time.LocalTime;

public record SalonEmployeeResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String specialty,
        String imageUrl,
        String status,
        LocalTime startTime,
        LocalTime endTime
) {
}
