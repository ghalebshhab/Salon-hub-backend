package com.salon.backend.Services.User;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;

import java.util.List;

public interface UserDirectoryService {
    ApiResponse<List<UserSummaryResponse>> getUsers(Long adminId);
    ApiResponse<List<UserSummaryResponse>> getActiveUsers(Long adminId);
    ApiResponse<List<UserSummaryResponse>> getBlockedUsers(Long adminId);
    ApiResponse<UserProfileResponse> getUserProfile(Long userId, Long adminId);
    ApiResponse<List<UserSummaryResponse>> getSalonTeam(Long salonId, Long adminId);
}
