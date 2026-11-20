package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;

import java.time.LocalDate;

public interface OwnerDashboardService {

    /** Every booking of the salon. Prefer the dated version for the dashboard page. */
    ApiResponse<OwnerDashboardResponse> getDashboard(Long ownerId);

    /**
     * Only bookings with an appointment date in the period, so the response
     * stays small for salons with a long history. Null dates mean every booking.
     */
    ApiResponse<OwnerDashboardResponse> getDashboard(Long ownerId, LocalDate from, LocalDate to);
}
