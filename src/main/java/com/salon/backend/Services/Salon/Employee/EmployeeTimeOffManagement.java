package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffResponse;

import java.util.List;

public interface EmployeeTimeOffManagement {

    ApiResponse<List<EmployeeTimeOffResponse>> getUpcomingTimeOff(Long employeeId, Long ownerId);

    ApiResponse<EmployeeTimeOffResponse> addTimeOff(
            Long employeeId,
            EmployeeTimeOffRequest request,
            Long ownerId
    );

    ApiResponse<Void> removeTimeOff(Long timeOffId, Long ownerId);
}
