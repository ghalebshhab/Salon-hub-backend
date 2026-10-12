package com.salon.backend.DTOs.Salon.Employee;

import java.math.BigDecimal;

public record EmployeeCapabilityServiceResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer durationMinutes,
        boolean active
) {
}
