package com.salon.backend.DTOs.Salon.Employee;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record EmployeeWorkingPeriodResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
