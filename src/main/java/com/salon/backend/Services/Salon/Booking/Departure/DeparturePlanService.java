package com.salon.backend.Services.Salon.Booking.Departure;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Departure.DeparturePlanResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;

import java.util.List;

/** "Leave on time": when the customer's turn really starts and when to leave. Customer only. */
public interface DeparturePlanService {

    ApiResponse<DeparturePlanResponse> getPlan(Long bookingId, Long customerId);

    /** Saves where the customer leaves from and turns the leave alerts on. */
    ApiResponse<DeparturePlanResponse> setOrigin(Long bookingId, CustomerPositionRequest origin, Long customerId);

    /** Forgets the saved place and turns the leave alerts off. */
    ApiResponse<DeparturePlanResponse> clearOrigin(Long bookingId, Long customerId);

    /** Today's bookings with alerts on, for the alert job. */
    List<Long> findAlertCandidateIds();

    /** Sends the leave-soon, leave-now and salon-delay alerts that are due. Each alert is sent once. */
    void sendDueAlerts(Long bookingId);
}
