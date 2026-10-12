package com.salon.backend.DTOs.Salon.Employee;

import java.time.LocalTime;
import java.util.List;

public record SalonEmployeeResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String specialty,
        String imageUrl,
        String status,
        LocalTime startTime,
        LocalTime endTime,
        List<Long> serviceIds,
        List<String> serviceNames
) {
}
