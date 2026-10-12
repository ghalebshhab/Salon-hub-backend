package com.salon.backend.DTOs.Salon.Employee;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record EmployeeWorkingPeriodRequest(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
