package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalResponse;

public interface BookingRescheduleManagement {

    ApiResponse<BookingAvailabilityResponse> getAvailability(
            Long bookingId,
            BookingRescheduleAvailabilityRequest request,
            Long ownerId
    );

    ApiResponse<BookingRescheduleProposalResponse> propose(
            Long bookingId,
            BookingRescheduleProposalRequest request,
            Long ownerId
    );

    ApiResponse<BookingRescheduleProposalResponse> accept(
            Long bookingId,
            Long proposalId,
            Long customerId
    );

    ApiResponse<BookingRescheduleProposalResponse> reject(
            Long bookingId,
            Long proposalId,
            Long customerId
    );
}
