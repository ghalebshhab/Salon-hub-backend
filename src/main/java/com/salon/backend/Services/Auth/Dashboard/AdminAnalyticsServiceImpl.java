package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.AdminAnalyticsResponse;
import com.salon.backend.DTOs.Auth.Dashboard.AdminMonthlyTrendResponse;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final UserRepo userRepo;
    private final SalonRepo salonRepo;
    private final BookingRepo bookingRepo;
    private final AdminAuthorizationService adminAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<AdminAnalyticsResponse> getAnalytics(Long adminId) {
        var authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) return ApiResponse.error(authorization.getMessage());

        LocalDateTime now = LocalDateTime.now();
        YearMonth firstMonth = YearMonth.from(now).minusMonths(5);
        Map<YearMonth, MonthlyAccumulator> trend = new LinkedHashMap<>();
        for (int index = 0; index < 6; index++) {
            trend.put(firstMonth.plusMonths(index), new MonthlyAccumulator());
        }

        List<Booking> recentBookings = bookingRepo.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                firstMonth.atDay(1).atStartOfDay()
        );
        for (Booking booking : recentBookings) {
            YearMonth month = YearMonth.from(booking.getCreatedAt());
            MonthlyAccumulator accumulator = trend.get(month);
            if (accumulator == null) continue;
            accumulator.bookings++;
            if (booking.getStatus() == BookingStatus.COMPLETED) accumulator.completedBookings++;
            accumulator.grossValue = accumulator.grossValue.add(zeroIfNull(booking.getTotalPrice()));
        }

        List<AdminMonthlyTrendResponse> monthlyTrend = trend.entrySet().stream()
                .map(entry -> new AdminMonthlyTrendResponse(
                        entry.getKey().format(MONTH_FORMAT),
                        entry.getValue().bookings,
                        entry.getValue().completedBookings,
                        entry.getValue().grossValue
                ))
                .toList();

        AdminAnalyticsResponse response = new AdminAnalyticsResponse(
                userRepo.count(),
                userRepo.countByStatus(UserStatus.Active),
                userRepo.countByStatus(UserStatus.Blocked),
                userRepo.countByRole(UserRole.OWNER),
                userRepo.countByRole(UserRole.Employee),
                userRepo.countByCareerProfileEnabledTrue(),
                salonRepo.count(),
                salonRepo.countByStatus(SalonStatus.Accepted),
                salonRepo.countByStatus(SalonStatus.Requested),
                salonRepo.countByStatus(SalonStatus.Rejected),
                salonRepo.countByStatus(SalonStatus.Inactive),
                salonRepo.countBlockedSalons(),
                bookingRepo.count(),
                bookingRepo.countByStatus(BookingStatus.PENDING),
                bookingRepo.countByStatus(BookingStatus.ACCEPTED),
                bookingRepo.countByStatus(BookingStatus.COMPLETED),
                bookingRepo.countByStatus(BookingStatus.REJECTED),
                bookingRepo.countByStatus(BookingStatus.CANCELLED),
                zeroIfNull(bookingRepo.sumGrossBookingValue()),
                zeroIfNull(bookingRepo.sumBookingValueByStatus(BookingStatus.COMPLETED)),
                monthlyTrend,
                now
        );
        return ApiResponse.success("Admin analytics loaded", response);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class MonthlyAccumulator {
        private long bookings;
        private long completedBookings;
        private BigDecimal grossValue = BigDecimal.ZERO;
    }
}
