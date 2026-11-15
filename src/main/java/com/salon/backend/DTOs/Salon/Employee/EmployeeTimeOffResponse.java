package com.salon.backend.DTOs.Salon.Employee;

import java.time.LocalDate;

/**
 * {@code affectedBookings} counts confirmed or pending appointments of this
 * employee inside the new time off, so the owner knows to reassign them.
 */
public record EmployeeTimeOffResponse(
        Long id,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        int affectedBookings
) {
}
