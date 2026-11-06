package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;
import com.salon.backend.Services.Salon.Booking.BookingManagement;
import com.salon.backend.Services.Salon.Employee.SalonEmployeeDirectory;
import com.salon.backend.Services.Salon.Review.SalonReviewManagement;
import com.salon.backend.Services.Salon.SalonService;
import com.salon.backend.Services.Salon.Service.SalonServiceManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private final SalonService salonService;
    private final BookingManagement bookingManagement;
    private final SalonEmployeeDirectory salonEmployeeDirectory;
    private final SalonServiceManagement salonServiceManagement;
    private final SalonReviewManagement salonReviewManagement;

    @Override
    public ApiResponse<OwnerDashboardResponse> getDashboard(Long ownerId) {
        if (ownerId == null) {
            return ApiResponse.error("Authenticated salon owner is required.");
        }

        ApiResponse<SalonResponse> salonResponse = salonService.getSalonByOwnerId(ownerId);
        if (!salonResponse.isSuccess() || salonResponse.getData() == null) {
            return ApiResponse.error(salonResponse.getMessage());
        }

        ApiResponse<List<BookingResponse>> bookingsResponse =
                bookingManagement.getMySalonBookings(ownerId);
        if (!bookingsResponse.isSuccess()) {
            return ApiResponse.error(bookingsResponse.getMessage());
        }

        ApiResponse<List<SalonEmployeeResponse>> employeesResponse =
                salonEmployeeDirectory.getMySalonEmployees(ownerId);
        if (!employeesResponse.isSuccess()) {
            return ApiResponse.error(employeesResponse.getMessage());
        }

        ApiResponse<List<SalonServiceResponse>> servicesResponse =
                salonServiceManagement.getMySalonServices(ownerId);
        if (!servicesResponse.isSuccess()) {
            return ApiResponse.error(servicesResponse.getMessage());
        }

        Long salonId = salonResponse.getData().id();
        ApiResponse<List<SalonReviewResponse>> reviewsResponse =
                salonReviewManagement.getSalonReviews(salonId);
        if (!reviewsResponse.isSuccess()) {
            return ApiResponse.error(reviewsResponse.getMessage());
        }

        ApiResponse<SalonReviewSummaryResponse> reviewSummaryResponse =
                salonReviewManagement.getSalonReviewSummary(salonId);
        if (!reviewSummaryResponse.isSuccess()) {
            return ApiResponse.error(reviewSummaryResponse.getMessage());
        }

        OwnerDashboardResponse dashboard = new OwnerDashboardResponse(
                salonResponse.getData(),
                safeList(bookingsResponse.getData()),
                safeList(employeesResponse.getData()),
                safeList(servicesResponse.getData()),
                safeList(reviewsResponse.getData()),
                reviewSummaryResponse.getData()
        );
        return ApiResponse.success("Owner dashboard fetched successfully.", dashboard);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
