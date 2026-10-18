package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRescheduleProposalRequest(
        LocalDate proposedDate,
        LocalTime proposedTime,
        String note
) {
}
