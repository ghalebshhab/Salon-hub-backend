package com.salon.backend.Services.Auth.Dashboard;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;

public interface DashboardService {
    ApiResponse<DashboardLoginResponse>  DashboardLogin(DashboardLoginRequest dashboardLoginRequest);
    ApiResponse<SalonResponse> acceptSalonRequest(Long salonId, String adminEmail);
    ApiResponse<SalonResponse> rejectSalonRequest(Long salonId, String adminEmail, String rejectionReason);
    ApiResponse<String> blockUser(Long userId);
}
