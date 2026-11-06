package com.salon.backend.DTOs.Auth.Dashboard;

public record DashboardSessionResponse(
        Long adminId,
        String email,
        String role
) {}
