package com.salon.backend.DTOs.Salon.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalonServiceResponse(
        Long id,
        Long salonId,
        String name,
        String description,
        String nameAr,
        String descriptionAr,
        BigDecimal price,
        Integer durationMinutes,
        @JsonProperty("isActive") boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
