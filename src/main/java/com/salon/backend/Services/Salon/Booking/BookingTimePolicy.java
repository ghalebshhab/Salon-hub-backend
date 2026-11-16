package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.Booking;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Time rules of the booking life cycle, in one place so the scheduler, the API
 * responses and the tests use the same values.
 */
public final class BookingTimePolicy {

    /** The salon must answer a pending request within this time. */
    public static final Duration RESPONSE_WINDOW = Duration.ofHours(24);
    /** First reminder to the customer. */
    public static final Duration DAY_REMINDER_BEFORE = Duration.ofHours(24);
    /** Second reminder, to the customer and the assigned employee. */
    public static final Duration SOON_REMINDER_BEFORE = Duration.ofHours(2);
    /** Staff can record a no-show this long after the start time. */
    public static final Duration NO_SHOW_GRACE = Duration.ofMinutes(15);

    private BookingTimePolicy() {
    }

    public static LocalDateTime appointmentStart(Booking booking) {
        return LocalDateTime.of(booking.getBookingDate(), booking.getBookingTime());
    }

    /**
     * Latest time for the salon to accept or reject a pending request: 24 hours
     * after the request, but never later than the appointment start.
     */
    public static LocalDateTime responseDeadline(Booking booking) {
        LocalDateTime start = appointmentStart(booking);
        if (booking.getCreatedAt() == null) {
            return start;
        }
        LocalDateTime windowEnd = booking.getCreatedAt().plus(RESPONSE_WINDOW);
        return windowEnd.isBefore(start) ? windowEnd : start;
    }

    public static boolean isResponseOverdue(Booking booking, LocalDateTime now) {
        return !now.isBefore(responseDeadline(booking));
    }

    public static LocalDateTime noShowAllowedFrom(Booking booking) {
        return appointmentStart(booking).plus(NO_SHOW_GRACE);
    }

    /** Which reminder is due now, or null. A later reminder replaces an earlier one. */
    public static ReminderType dueReminder(Booking booking, LocalDateTime now) {
        LocalDateTime start = appointmentStart(booking);
        if (!now.isBefore(start)) {
            return null;
        }
        if (!now.isBefore(start.minus(SOON_REMINDER_BEFORE))) {
            return ReminderType.SOON;
        }
        if (!now.isBefore(start.minus(DAY_REMINDER_BEFORE))) {
            return ReminderType.DAY;
        }
        return null;
    }

    public enum ReminderType {
        /** About one day before. */
        DAY,
        /** About two hours before. */
        SOON
    }
}
