package com.salon.backend.DTOs.Owner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Totals for the owner dashboard, counted in SQL for one period and the
 * period it is compared with. Dates are appointment dates, both inclusive.
 * The comparison period may reach before {@code historyDays}, because it only
 * feeds the change shown next to each total.
 */
public record OwnerDashboardSummaryResponse(
        LocalDate from,
        LocalDate to,
        LocalDate compareFrom,
        LocalDate compareTo,
        PeriodTotals current,
        PeriodTotals previous,
        CustomerTotals customers,
        PaymentTotals payments,
        List<DayPoint> days,
        List<HourPoint> busyHours,
        String currency,
        /** How many days back the salon's plan may look, counted from today. */
        int historyDays,
        /**
         * False when the plan has no advanced analytics; then
         * {@code customers}, {@code payments} and {@code busyHours} are null.
         */
        boolean advancedAnalytics
) {

    /** Booking counts by result, and the money from completed visits. */
    public record PeriodTotals(
            long bookings,
            long completed,
            long pending,
            long confirmed,
            long cancelled,
            long noShow,
            long declined,
            long expired,
            BigDecimal revenue,
            BigDecimal averageValue,
            /** Cancelled and no-show bookings as a percent of all bookings, one decimal. */
            double lostRate,
            /** No-show bookings as a percent of bookings the salon confirmed, one decimal. */
            double noShowRate
    ) {
    }

    /** People who booked in the period, and how many of them visited before. */
    public record CustomerTotals(
            long unique,
            long returning,
            double returningRate
    ) {
    }

    /** Payment state of the completed visits in the period, in fils. */
    public record PaymentTotals(
            long paidFils,
            long paidCount,
            long unpaidFils,
            long unpaidCount,
            long refundedFils
    ) {
    }

    /** One calendar day of the period. Days without bookings are included with zeros. */
    public record DayPoint(
            LocalDate date,
            long bookings,
            long completed,
            BigDecimal revenue
    ) {
    }

    /**
     * Appointments that took a chair (confirmed, in service, completed, no-show)
     * per weekday and start hour. ISO weekday: 1 is Monday, 7 is Sunday.
     */
    public record HourPoint(
            int dayOfWeek,
            int hour,
            long bookings
    ) {
    }
}
