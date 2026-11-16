package com.salon.backend.DTOs.Media;

import com.salon.backend.Entities.media.MediaResourceType;

import java.time.LocalDateTime;

public record EmployeeWorkMediaResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String employeeSpecialty,
        String secureUrl,
        MediaResourceType resourceType,
        String caption,
        Integer width,
        Integer height,
        Double durationSeconds,
        LocalDateTime createdAt
) {}
