package com.salon.backend.DTOs.Salon.Closure;

import java.time.LocalDate;

/**
 * {@code affectedBookings} counts pending and confirmed appointments inside the
 * closure. It is filled only for the owner; the public list returns null.
 */
public record SalonClosureResponse(
        Long id,
        Long salonId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        Integer affectedBookings
) {
}
