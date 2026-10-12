package com.salon.backend.DTOs.Salon.Employee;

import java.time.LocalTime;
import java.util.List;

public record EmployeeScheduleResponse(
        Long employeeId,
        String employeeName,
        Long salonId,
        boolean configured,
        LocalTime salonOpeningTime,
        LocalTime salonClosingTime,
        List<EmployeeWorkingPeriodResponse> periods
) {
}
