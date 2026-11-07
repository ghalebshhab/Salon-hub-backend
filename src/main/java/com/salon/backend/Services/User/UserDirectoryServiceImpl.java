package com.salon.backend.Services.User;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import com.salon.backend.Services.User.Profile.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDirectoryServiceImpl implements UserDirectoryService {

    private final UserRepo userRepo;
    private final AdminAuthorizationService adminAuthorizationService;
    private final UserProfileService userProfileService;

    @Override
    public ApiResponse<List<UserSummaryResponse>> getUsers(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        return ApiResponse.success("Users Fetched Successfully.", mapUsers(userRepo.findAll()));
    }

    @Override
    public ApiResponse<List<UserSummaryResponse>> getActiveUsers(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        return ApiResponse.success(
                "Active Users Fetched Successfully.",
                mapUsers(userRepo.findByStatus(UserStatus.Active))
        );
    }

    @Override
    public ApiResponse<List<UserSummaryResponse>> getBlockedUsers(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        return ApiResponse.success(
                "Blocked Users Fetched Successfully.",
                mapUsers(userRepo.findByStatus(UserStatus.Blocked))
        );
    }

    @Override
    public ApiResponse<UserProfileResponse> getUserProfile(Long userId, Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        if (userId == null) {
            return ApiResponse.error("User id is required");
        }
        return userProfileService.getProfile(userId);
    }

    @Override
    public ApiResponse<List<UserSummaryResponse>> getSalonTeam(Long salonId, Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }
        return ApiResponse.success(
                "Salon team fetched successfully",
                mapUsers(userRepo.findBySalonIdAndRoleOrderByFirstNameAscLastNameAsc(
                        salonId,
                        com.salon.backend.Entities.users.UserRole.Employee
                ))
        );
    }

    private List<UserSummaryResponse> mapUsers(List<User> users) {
        return users.stream().map(this::mapUser).toList();
    }

    private UserSummaryResponse mapUser(User user) {
        Salon salon = user.getSalon();
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getUserName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                salon == null ? null : salon.getId(),
                salon == null ? null : salon.getName(),
                user.isCareerProfileEnabled()
        );
    }
}
