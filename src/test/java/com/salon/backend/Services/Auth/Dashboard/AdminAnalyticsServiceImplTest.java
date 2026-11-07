package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {
    @Mock private UserRepo userRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private BookingRepo bookingRepo;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @InjectMocks private AdminAnalyticsServiceImpl service;

    @Test
    void buildsAnalyticsFromPersistedPlatformData() {
        User admin = new User();
        admin.setId(8L);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Active);
        when(adminAuthorizationService.authorize(8L))
                .thenReturn(ApiResponse.success("Authorized", admin));
        when(userRepo.count()).thenReturn(20L);
        when(userRepo.countByStatus(UserStatus.Active)).thenReturn(17L);
        when(salonRepo.count()).thenReturn(5L);
        when(bookingRepo.count()).thenReturn(9L);
        when(bookingRepo.sumGrossBookingValue()).thenReturn(new BigDecimal("240.00"));
        when(bookingRepo.sumBookingValueByStatus(BookingStatus.COMPLETED))
                .thenReturn(new BigDecimal("150.00"));

        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalPrice(new BigDecimal("60.00"));
        when(bookingRepo.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(booking));

        var response = service.getAnalytics(8L);

        assertTrue(response.isSuccess());
        assertEquals(20L, response.getData().totalUsers());
        assertEquals(5L, response.getData().totalSalons());
        assertEquals(9L, response.getData().totalBookings());
        assertEquals(new BigDecimal("240.00"), response.getData().grossBookingValue());
        assertEquals(6, response.getData().monthlyTrend().size());
        assertEquals(1L, response.getData().monthlyTrend().get(5).bookings());
    }
}
