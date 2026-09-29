package com.salon.backend.DTOs.Salon.Employment.Leave;

import com.salon.backend.Entities.salons.employment.LeaveRequestStatus;

import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long requestId,
        Long employeeId,
        String employeeName,
        Long currentSalonId,
        String currentSalonName,
        Long targetSalonId,
        String targetSalonName,
        Long employmentRequestId,
        LeaveRequestStatus status,
        String reason,
        LocalDateTime createdAt
) {}
