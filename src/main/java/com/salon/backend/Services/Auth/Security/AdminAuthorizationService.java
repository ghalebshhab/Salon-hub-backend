package com.salon.backend.Services.Auth.Security;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final UserRepo userRepo;

    public ApiResponse<User> authorize(Long adminId) {
        if (adminId == null) {
            return ApiResponse.error("Admin not found");
        }

        User admin = userRepo.findById(adminId).orElse(null);
        if (admin == null) {
            return ApiResponse.error("Admin not found");
        }

        if (admin.getRole() != UserRole.ADMIN) {
            return ApiResponse.error("You are not Admin , Access Denied");
        }

        if (admin.getStatus() != UserStatus.Active) {
            return ApiResponse.error("Admin account is not active");
        }

        return ApiResponse.success("Admin authorized", admin);
    }
}
