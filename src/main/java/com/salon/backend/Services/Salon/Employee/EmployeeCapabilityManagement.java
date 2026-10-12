package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeCapabilitiesResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeServicesRequest;

public interface EmployeeCapabilityManagement {

    ApiResponse<EmployeeCapabilitiesResponse> getEmployeeServices(Long employeeId, Long ownerId);

    ApiResponse<EmployeeCapabilitiesResponse> updateEmployeeServices(
            Long employeeId,
            UpdateEmployeeServicesRequest request,
            Long ownerId
    );
}
