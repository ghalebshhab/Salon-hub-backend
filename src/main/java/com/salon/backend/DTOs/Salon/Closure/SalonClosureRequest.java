package com.salon.backend.DTOs.Salon.Closure;

import java.time.LocalDate;

/** Whole days the salon is closed. Both dates are inclusive; the reason is shown to customers. */
public record SalonClosureRequest(LocalDate startDate, LocalDate endDate, String reason) {
}
