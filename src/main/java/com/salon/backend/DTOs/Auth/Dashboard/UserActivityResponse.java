package com.salon.backend.DTOs.Auth.Dashboard;

import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;

import java.time.Instant;

public record UserActivityResponse(
        Long userId,
        String fullName,
        String userName,
        String email,
        UserRole role,
        UserStatus status,
        boolean online,
        Instant lastLoginAt,
        Instant lastActivityAt,
        long totalLogins,
        long loginsToday,
        long loginsThisWeek,
        long loginsThisMonth,
        long loginsThisYear,
        long activityToday,
        long activityThisWeek,
        long activityThisMonth,
        long activityThisYear
) {}
