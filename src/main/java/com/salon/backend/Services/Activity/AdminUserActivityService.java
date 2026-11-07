package com.salon.backend.Services.Activity;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.UserActivityDashboardResponse;

public interface AdminUserActivityService {
    ApiResponse<UserActivityDashboardResponse> getUserActivity(Long adminId);
}
