package com.salon.backend.DTOs.User;

import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;

public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String userName,
        String phoneNumber,
        UserRole role,
        UserStatus status,
        Long salonId,
        String salonName,
        boolean careerProfileEnabled
) {}
