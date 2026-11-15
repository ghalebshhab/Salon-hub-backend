package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;
import java.util.List;

/**
 * When {@code slots} is empty, {@code unavailableReason} says why
 * ({@link BookingUnavailableReason}) and {@code nextAvailableDate} is the first
 * later day with at least one free time inside the search window, or null.
 */
public record BookingAvailabilityResponse(
        Long salonId,
        LocalDate bookingDate,
        int totalDurationMinutes,
        int slotIntervalMinutes,
        int minimumLeadMinutes,
        List<BookingAvailabilitySlotResponse> slots,
        String unavailableReason,
        LocalDate nextAvailableDate
) {
}
