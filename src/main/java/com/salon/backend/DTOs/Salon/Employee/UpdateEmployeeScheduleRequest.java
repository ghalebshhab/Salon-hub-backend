package com.salon.backend.DTOs.Salon.Employee;

import java.util.List;

public record UpdateEmployeeScheduleRequest(List<EmployeeWorkingPeriodRequest> periods) {
}
