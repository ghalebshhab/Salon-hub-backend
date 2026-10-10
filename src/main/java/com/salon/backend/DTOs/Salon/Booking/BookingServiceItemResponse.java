package com.salon.backend.DTOs.Salon.Booking;

import java.math.BigDecimal;

public record BookingServiceItemResponse(
        Long id,
        Long serviceId,
        String name,
        BigDecimal price,
        Integer durationMinutes
) {
}
