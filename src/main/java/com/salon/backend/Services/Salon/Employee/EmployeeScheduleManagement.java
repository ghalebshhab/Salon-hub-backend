package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeScheduleResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeScheduleRequest;

public interface EmployeeScheduleManagement {

    ApiResponse<EmployeeScheduleResponse> getEmployeeSchedule(Long employeeId, Long ownerId);

    ApiResponse<EmployeeScheduleResponse> updateEmployeeSchedule(
            Long employeeId,
            UpdateEmployeeScheduleRequest request,
            Long ownerId
    );
}
