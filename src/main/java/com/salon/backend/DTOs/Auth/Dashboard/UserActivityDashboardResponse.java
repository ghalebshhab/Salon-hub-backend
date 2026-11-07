package com.salon.backend.DTOs.Auth.Dashboard;

import java.time.Instant;
import java.util.List;

public record UserActivityDashboardResponse(
        long totalUsers,
        long onlineUsers,
        long activeToday,
        long activeThisWeek,
        long activeThisMonth,
        Instant generatedAt,
        List<UserActivityResponse> users
) {}
