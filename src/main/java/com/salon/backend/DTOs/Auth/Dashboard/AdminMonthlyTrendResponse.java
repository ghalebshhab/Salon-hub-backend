package com.salon.backend.DTOs.Auth.Dashboard;

import java.math.BigDecimal;

public record AdminMonthlyTrendResponse(
        String month,
        long bookings,
        long completedBookings,
        BigDecimal grossBookingValue
) {}
