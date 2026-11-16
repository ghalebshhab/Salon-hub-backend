package com.salon.backend.Services.Salon.Booking.Tracking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.BookingTrackingResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.DTOs.Salon.Booking.Tracking.ExtendServiceRequest;

public interface BookingTrackingService {

    ApiResponse<BookingTrackingResponse> getTracking(Long bookingId, Long viewerId);

    /** Customer: "I'm on my way". Position is optional. */
    ApiResponse<BookingTrackingResponse> startJourney(Long bookingId, CustomerPositionRequest position, Long customerId);

    ApiResponse<BookingTrackingResponse> updatePosition(Long bookingId, CustomerPositionRequest position, Long customerId);

    ApiResponse<BookingTrackingResponse> markArrived(Long bookingId, Long customerId);

    /** Customer stops sharing; stored coordinates are cleared. */
    ApiResponse<BookingTrackingResponse> stopJourney(Long bookingId, Long customerId);

    /** Assigned employee or owner. */
    ApiResponse<BookingTrackingResponse> startService(Long bookingId, Long staffId);

    ApiResponse<BookingTrackingResponse> extendService(Long bookingId, ExtendServiceRequest request, Long staffId);

    ApiResponse<BookingTrackingResponse> completeService(Long bookingId, Long staffId);

    /** Records that the customer did not come. Staff only, from 15 minutes after the start. */
    ApiResponse<BookingTrackingResponse> markNoShow(Long bookingId, Long staffId);
}
