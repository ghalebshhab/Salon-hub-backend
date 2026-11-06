package com.salon.backend.DTOs.Owner;

import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;

import java.util.List;

public record OwnerDashboardResponse(
        SalonResponse salon,
        List<BookingResponse> bookings,
        List<SalonEmployeeResponse> employees,
        List<SalonServiceResponse> services,
        List<SalonReviewResponse> reviews,
        SalonReviewSummaryResponse reviewSummary
) {
}
