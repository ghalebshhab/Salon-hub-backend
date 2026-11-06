package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Services.Salon.Booking.BookingManagement;
import com.salon.backend.Services.Salon.Employee.SalonEmployeeDirectory;
import com.salon.backend.Services.Salon.Review.SalonReviewManagement;
import com.salon.backend.Services.Salon.SalonService;
import com.salon.backend.Services.Salon.Service.SalonServiceManagement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerDashboardServiceImplTest {

    @Mock private SalonService salonService;
    @Mock private BookingManagement bookingManagement;
    @Mock private SalonEmployeeDirectory salonEmployeeDirectory;
    @Mock private SalonServiceManagement salonServiceManagement;
    @Mock private SalonReviewManagement salonReviewManagement;

    private OwnerDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OwnerDashboardServiceImpl(
                salonService,
                bookingManagement,
                salonEmployeeDirectory,
                salonServiceManagement,
                salonReviewManagement
        );
    }

    @Test
    void buildsOneDashboardResponseFromExistingOwnerServices() {
        SalonResponse salon = new SalonResponse(
                12L,
                "Maison Rose",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                5L,
                null,
                null,
                0L,
                5L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                3
        );
        when(salonService.getSalonByOwnerId(5L))
                .thenReturn(ApiResponse.success("Salon loaded", salon));
        when(bookingManagement.getMySalonBookings(5L))
                .thenReturn(ApiResponse.success("Bookings loaded", List.of()));
        when(salonEmployeeDirectory.getMySalonEmployees(5L))
                .thenReturn(ApiResponse.success("Employees loaded", List.of()));
        when(salonServiceManagement.getMySalonServices(5L))
                .thenReturn(ApiResponse.success("Services loaded", List.of()));
        when(salonReviewManagement.getSalonReviews(12L))
                .thenReturn(ApiResponse.success("Reviews loaded", List.of()));
        when(salonReviewManagement.getSalonReviewSummary(12L))
                .thenReturn(ApiResponse.success(
                        "Summary loaded",
                        new SalonReviewSummaryResponse(4.8, 20)
                ));

        ApiResponse<OwnerDashboardResponse> response = service.getDashboard(5L);

        assertTrue(response.isSuccess());
        assertEquals(salon, response.getData().salon());
        assertEquals(4.8, response.getData().reviewSummary().averageRating());
        verify(salonReviewManagement).getSalonReviews(12L);
    }

    @Test
    void returnsTheExistingSalonErrorWithoutLoadingOtherDashboardData() {
        when(salonService.getSalonByOwnerId(5L))
                .thenReturn(ApiResponse.error("Salon owned by you was not found."));

        ApiResponse<OwnerDashboardResponse> response = service.getDashboard(5L);

        assertFalse(response.isSuccess());
        assertEquals("Salon owned by you was not found.", response.getMessage());
        verify(bookingManagement, never()).getMySalonBookings(5L);
    }
}
