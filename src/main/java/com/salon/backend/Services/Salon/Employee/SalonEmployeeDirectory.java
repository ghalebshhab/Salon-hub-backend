package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;

import java.util.List;

public interface SalonEmployeeDirectory {

    ApiResponse<List<SalonEmployeeResponse>> getSalonEmployees(Long salonId);

    ApiResponse<List<SalonEmployeeResponse>> getMySalonEmployees(Long ownerId);
}
