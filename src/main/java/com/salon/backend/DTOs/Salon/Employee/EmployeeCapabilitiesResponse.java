package com.salon.backend.DTOs.Salon.Employee;

import java.util.List;

public record EmployeeCapabilitiesResponse(
        Long employeeId,
        String employeeName,
        Long salonId,
        List<EmployeeCapabilityServiceResponse> services
) {
}
