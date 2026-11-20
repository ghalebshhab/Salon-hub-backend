package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse.CustomerTotals;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse.DayPoint;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse.HourPoint;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse.PaymentTotals;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse.PeriodTotals;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Services.Subscription.PlanLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Owner dashboard numbers. The database does the counting with GROUP BY,
 * so the response size depends on the number of days, not on the bookings.
 */
@Service
@RequiredArgsConstructor
public class OwnerDashboardSummaryService {

    static final int MAX_RANGE_DAYS = 366;
    static final int DEFAULT_RANGE_DAYS = 7;

    /** Appointments that used a chair and a professional's time. */
    private static final Set<BookingStatus> CHAIR_STATUSES = EnumSet.of(
            BookingStatus.ACCEPTED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED,
            BookingStatus.NO_SHOW
    );
    /** Bookings the salon confirmed; the base of the no-show rate. */
    private static final Set<BookingStatus> CONFIRMED_STATUSES = CHAIR_STATUSES;

    private final BookingRepo bookingRepo;
    private final SalonRepo salonRepo;
    private final Clock clock;
    private final PlanLimits planLimits;

    @Transactional(readOnly = true)
    public ApiResponse<OwnerDashboardSummaryResponse> getSummary(
            Long ownerId,
            LocalDate from,
            LocalDate to,
            LocalDate compareFrom,
            LocalDate compareTo) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }

        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_RANGE_DAYS - 1L) : from;
        String rangeError = validateRange(start, end);
        if (rangeError != null) {
            return ApiResponse.error(rangeError);
        }
        int historyDays = historyDays(salon);
        LocalDate oldestAllowed = LocalDate.now(clock).minusDays(historyDays - 1L);
        if (start.isBefore(oldestAllowed) || ChronoUnit.DAYS.between(start, end) + 1 > historyDays) {
            return ApiResponse.error("The " + planLimits.plan(salon).getDisplayName()
                    + " plan shows the last " + historyDays + " days. Upgrade your plan to see a longer period.");
        }
        boolean advanced = !planLimits.locked(salon, PlanFeature.ADVANCED_ANALYTICS);

        // Without an explicit comparison, use the same number of days just before.
        long length = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = compareTo == null ? start.minusDays(1) : compareTo;
        LocalDate previousStart = compareFrom == null ? previousEnd.minusDays(length - 1) : compareFrom;
        String compareError = validateRange(previousStart, previousEnd);
        if (compareError != null) {
            return ApiResponse.error("Comparison period: " + compareError.toLowerCase());
        }

        Long salonId = salon.getId();
        List<Object[]> currentRows = bookingRepo.sumByDayAndStatus(salonId, start, end);
        List<Object[]> previousRows = bookingRepo.sumByDayAndStatus(salonId, previousStart, previousEnd);

        return ApiResponse.success("Owner dashboard summary fetched successfully", new OwnerDashboardSummaryResponse(
                start,
                end,
                previousStart,
                previousEnd,
                totals(currentRows),
                totals(previousRows),
                advanced ? customers(salonId, start, end) : null,
                advanced ? payments(salonId, start, end) : null,
                days(currentRows, start, end),
                advanced ? busyHours(salonId, start, end) : null,
                BookingPayment.CURRENCY,
                historyDays,
                advanced
        ));
    }

    /** The plan's history in days; never more than the hard limit. */
    private int historyDays(Salon salon) {
        int days = planLimits.analyticsMaxDays(salon);
        return days <= 0 ? MAX_RANGE_DAYS : Math.min(days, MAX_RANGE_DAYS);
    }

    private static String validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            return "The start date cannot be after the end date";
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            return "Choose a period of at most one year";
        }
        return null;
    }

    /** Rows are: date, status, count, booking value. */
    private static PeriodTotals totals(List<Object[]> rows) {
        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        BigDecimal revenue = BigDecimal.ZERO;
        long all = 0;
        for (Object[] row : rows) {
            BookingStatus status = (BookingStatus) row[1];
            long count = number(row[2]);
            counts.merge(status, count, Long::sum);
            all += count;
            if (status == BookingStatus.COMPLETED) {
                revenue = revenue.add(money(row[3]));
            }
        }
        long completed = counts.getOrDefault(BookingStatus.COMPLETED, 0L);
        long cancelled = counts.getOrDefault(BookingStatus.CANCELLED, 0L);
        long noShow = counts.getOrDefault(BookingStatus.NO_SHOW, 0L);
        long confirmed = CONFIRMED_STATUSES.stream()
                .mapToLong(status -> counts.getOrDefault(status, 0L))
                .sum();
        BigDecimal average = completed == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);

        return new PeriodTotals(
                all,
                completed,
                counts.getOrDefault(BookingStatus.PENDING, 0L),
                counts.getOrDefault(BookingStatus.ACCEPTED, 0L)
                        + counts.getOrDefault(BookingStatus.IN_PROGRESS, 0L),
                cancelled,
                noShow,
                counts.getOrDefault(BookingStatus.REJECTED, 0L),
                counts.getOrDefault(BookingStatus.EXPIRED, 0L),
                revenue,
                average,
                percent(cancelled + noShow, all),
                percent(noShow, confirmed)
        );
    }

    /** Every day of the period, with zeros for days without bookings. */
    private static List<DayPoint> days(List<Object[]> rows, LocalDate start, LocalDate end) {
        Map<LocalDate, long[]> counts = new TreeMap<>();
        Map<LocalDate, BigDecimal> revenue = new TreeMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            BookingStatus status = (BookingStatus) row[1];
            long count = number(row[2]);
            long[] dayCounts = counts.computeIfAbsent(date, ignored -> new long[2]);
            dayCounts[0] += count;
            if (status == BookingStatus.COMPLETED) {
                dayCounts[1] += count;
                revenue.merge(date, money(row[3]), BigDecimal::add);
            }
        }
        List<DayPoint> points = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long[] dayCounts = counts.getOrDefault(date, new long[2]);
            points.add(new DayPoint(
                    date,
                    dayCounts[0],
                    dayCounts[1],
                    revenue.getOrDefault(date, BigDecimal.ZERO)
            ));
        }
        return points;
    }

    /** Rows are: date, hour, count. Grouped again by weekday here. */
    private List<HourPoint> busyHours(Long salonId, LocalDate start, LocalDate end) {
        Map<Integer, Long> byWeekdayHour = new TreeMap<>();
        for (Object[] row : bookingRepo.countByDayAndHour(salonId, start, end, CHAIR_STATUSES)) {
            int weekday = ((LocalDate) row[0]).getDayOfWeek().getValue();
            int hour = (int) number(row[1]);
            byWeekdayHour.merge(weekday * 100 + hour, number(row[2]), Long::sum);
        }
        List<HourPoint> points = new ArrayList<>();
        byWeekdayHour.forEach((key, count) -> points.add(new HourPoint(key / 100, key % 100, count)));
        return points;
    }

    private CustomerTotals customers(Long salonId, LocalDate start, LocalDate end) {
        long unique = bookingRepo.countCustomers(salonId, start, end);
        long returning = bookingRepo.countReturningCustomers(salonId, start, end, BookingStatus.COMPLETED);
        return new CustomerTotals(unique, returning, percent(returning, unique));
    }

    /** Rows are: payment status, count, amount, refunded. */
    private PaymentTotals payments(Long salonId, LocalDate start, LocalDate end) {
        long paidFils = 0;
        long paidCount = 0;
        long unpaidFils = 0;
        long unpaidCount = 0;
        long refundedFils = 0;
        for (Object[] row : bookingRepo.sumPaymentsOfCompleted(salonId, start, end, BookingStatus.COMPLETED)) {
            PaymentStatus status = (PaymentStatus) row[0];
            long count = number(row[1]);
            long amount = number(row[2]);
            refundedFils += number(row[3]);
            if (status.isSettled() || status == PaymentStatus.REFUNDED) {
                paidFils += amount;
                paidCount += count;
            } else if (status == PaymentStatus.UNPAID
                    || status == PaymentStatus.AUTHORIZED
                    || status == PaymentStatus.FAILED) {
                unpaidFils += amount;
                unpaidCount += count;
            }
        }
        return new PaymentTotals(paidFils, paidCount, unpaidFils, unpaidCount, refundedFils);
    }

    private Salon ownedActiveSalon(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner()
                ? salon
                : null;
    }

    /** Percent with one decimal, 0 when there is nothing to divide by. */
    private static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal money(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
