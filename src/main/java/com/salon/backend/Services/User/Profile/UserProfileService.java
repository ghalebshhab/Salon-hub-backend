package com.salon.backend.Services.User.Profile;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.CareerProfileRequest;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;

public interface UserProfileService {

    ApiResponse<UserProfileResponse> getProfile(String email);

    ApiResponse<UserProfileResponse> enableOrUpdateCareerProfile(
            CareerProfileRequest request,
            String email
    );
}
