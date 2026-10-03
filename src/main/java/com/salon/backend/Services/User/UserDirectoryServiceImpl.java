package com.salon.backend.Services.User;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDirectoryServiceImpl implements UserDirectoryService {

    private final UserRepo userRepo;
    private final AdminAuthorizationService adminAuthorizationService;

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
