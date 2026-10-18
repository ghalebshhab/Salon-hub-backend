package com.salon.backend.DTOs.Salon.Booking;

import java.time.LocalDate;

public record BookingRescheduleAvailabilityRequest(LocalDate bookingDate) {
}
