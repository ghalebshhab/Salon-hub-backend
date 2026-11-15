package com.salon.backend.DTOs.Salon.Employee;

import java.time.LocalDate;

public record EmployeeTimeOffRequest(LocalDate startDate, LocalDate endDate, String reason) {
}
