package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.DTOs.Salon.Employee.PublicSalonEmployeeResponse;

import java.util.List;

public interface SalonEmployeeDirectory {

    ApiResponse<List<PublicSalonEmployeeResponse>> getSalonEmployees(Long salonId);

    ApiResponse<List<SalonEmployeeResponse>> getMySalonEmployees(Long ownerId);
}
