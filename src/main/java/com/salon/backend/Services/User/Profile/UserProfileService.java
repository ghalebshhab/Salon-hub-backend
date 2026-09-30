package com.salon.backend.Services.User.Profile;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.CareerProfileRequest;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;

public interface UserProfileService {

    ApiResponse<UserProfileResponse> getProfile(Long userId);

    ApiResponse<UserProfileResponse> enableOrUpdateCareerProfile(
            CareerProfileRequest request,
            Long userId
    );
}
