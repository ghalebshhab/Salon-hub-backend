package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.AdminAnalyticsResponse;

public interface AdminAnalyticsService {
    ApiResponse<AdminAnalyticsResponse> getAnalytics(Long adminId);
}
