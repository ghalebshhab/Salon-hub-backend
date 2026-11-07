package com.salon.backend.DTOs.Auth.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAnalyticsResponse(
        long totalUsers,
        long activeUsers,
        long blockedUsers,
        long owners,
        long employees,
        long careerProfiles,
        long totalSalons,
        long acceptedSalons,
        long requestedSalons,
        long rejectedSalons,
        long inactiveSalons,
        long blockedSalons,
        long totalBookings,
        long pendingBookings,
        long acceptedBookings,
        long completedBookings,
        long rejectedBookings,
        long cancelledBookings,
        BigDecimal grossBookingValue,
        BigDecimal completedBookingValue,
        List<AdminMonthlyTrendResponse> monthlyTrend,
        LocalDateTime generatedAt
) {}
