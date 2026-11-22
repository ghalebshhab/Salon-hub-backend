package com.salon.backend.DTOs.Salon.Booking;

import java.math.BigDecimal;

public record BookingServiceItemResponse(
        Long id,
        Long serviceId,
        String name,
        /** Arabic name of the service, or null when there is none. */
        String nameAr,
        BigDecimal price,
        Integer durationMinutes
) {
}
