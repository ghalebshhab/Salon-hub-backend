package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public interface BookingAvailabilityService {

    ApiResponse<BookingAvailabilityResponse> getAvailability(
            BookingAvailabilityRequest request,
            Long requesterId
    );

    ApiResponse<BookingAvailabilityResponse> getRescheduleAvailability(
            Booking booking,
            LocalDate bookingDate
    );

    boolean isSlotAvailable(
            Salon salon,
            Set<Long> serviceIds,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes,
            Long employeeId
    );

    boolean isSlotAvailable(
            Salon salon,
            Set<Long> serviceIds,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes,
            Long employeeId,
            Long excludedBookingId
    );
}
