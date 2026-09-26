package com.salon.backend.Controllers.Users;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.CareerProfileRequest;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
import com.salon.backend.Services.User.Profile.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(Authentication authentication) {
        return userProfileService.getProfile(authentication.getName());
    }

    @PutMapping("/career")
    public ApiResponse<UserProfileResponse> enableOrUpdateCareerProfile(
            @RequestBody CareerProfileRequest request,
            Authentication authentication) {

        return userProfileService.enableOrUpdateCareerProfile(
                request,
                authentication.getName()
        );
    }
}
