package com.salon.backend.Controllers.Users;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.CareerProfileRequest;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
import com.salon.backend.Services.User.Profile.UserProfileService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<UserProfileResponse> getProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userProfileService.getProfile(currentUser.getId());
    }

    @PutMapping("/career")
    public ApiResponse<UserProfileResponse> enableOrUpdateCareerProfile(
            @RequestBody CareerProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return userProfileService.enableOrUpdateCareerProfile(
                request,
                currentUser.getId()
        );
    }
}
