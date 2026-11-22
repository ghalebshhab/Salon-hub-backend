package com.salon.backend.Services.Salon.Booking.Departure;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.BookingTracking;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;
import com.salon.backend.Services.Salon.Booking.BookingTimePolicy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Predicts when the assigned employee will really be free for one booking.
 *
 * <p>It walks the employee's earlier appointments of the same day from now:
 * the client in the chair ends at the employee's expected end time, and each
 * later client starts when the chair is free (never before their own slot) and
 * takes their planned time multiplied by the employee's usual pace. Clients
 * who are late past the no-show grace and did not share a trip are skipped.
 *
 * <p>Pure and stateless, so the same rules serve the API and the alert job.
 */
public final class QueueForecaster {

    /** A service that passed its expected end is assumed to need this much more. */
    static final Duration OVERRUN_ALLOWANCE = Duration.ofMinutes(5);
    static final int DEFAULT_DURATION_MINUTES = 30;
    public static final double MIN_PACE = 0.85;
    public static final double MAX_PACE = 1.4;
    static final int MIN_PACE_SAMPLES = 3;

    private QueueForecaster() {
    }

    public enum Confidence { SCHEDULED, LOW, MEDIUM, HIGH }

    /**
     * @param expectedStart when the employee is expected to start this booking
     * @param delayMinutes  how much later than the booked slot (0 or more)
     * @param clientsAhead  clients served before this one from now on
     */
    public record Forecast(LocalDateTime expectedStart, int delayMinutes, int clientsAhead,
                           Confidence confidence, double pace) {
    }

    /** How fast the employee works: actual time divided by planned time. */
    public record Pace(double factor, int samples) {
        public static final Pace UNKNOWN = new Pace(1.0, 0);
    }

    /**
     * @param target   the booking to forecast
     * @param sameDay  the employee's ACCEPTED and IN_PROGRESS bookings on that day (may include the target)
     * @param pace     the employee's pace
     * @param now      the current time
     */
    public static Forecast forecast(Booking target, List<Booking> sameDay, Pace pace, LocalDateTime now) {
        LocalDateTime slot = BookingTimePolicy.appointmentStart(target);
        double factor = pace == null ? 1.0 : pace.factor();
        if (target.getAssignedEmployee() == null || !slot.toLocalDate().equals(now.toLocalDate())) {
            return new Forecast(slot, 0, 0, Confidence.SCHEDULED, factor);
        }

        List<Booking> ahead = new ArrayList<>();
        for (Booking other : sameDay) {
            if (other.getId() == null || other.getId().equals(target.getId())) {
                continue;
            }
            if (other.getStatus() == BookingStatus.IN_PROGRESS || isBefore(other, target)) {
                ahead.add(other);
            }
        }
        ahead.sort(Comparator.comparing(BookingTimePolicy::appointmentStart)
                .thenComparing(Booking::getId));

        LocalDateTime cursor = now;
        int clientsAhead = 0;
        for (Booking other : ahead) {
            if (other.getStatus() == BookingStatus.IN_PROGRESS) {
                LocalDateTime end = runningEnd(other, now);
                if (end.isAfter(cursor)) {
                    cursor = end;
                }
                clientsAhead++;
            } else if (other.getStatus() == BookingStatus.ACCEPTED && !likelyNoShow(other, now)) {
                LocalDateTime start = later(cursor, BookingTimePolicy.appointmentStart(other));
                start = later(start, expectedArrival(other));
                long minutes = Math.round(durationOf(other) * factor);
                cursor = start.plusMinutes(minutes);
                clientsAhead++;
            }
        }

        LocalDateTime expectedStart = later(cursor, slot);
        int delay = (int) Math.max(0, Duration.between(slot, expectedStart).toMinutes());
        return new Forecast(expectedStart, delay, clientsAhead, confidence(pace, clientsAhead), factor);
    }

    /** Median of actual / planned service time over recent timed services, clamped to a safe range. */
    public static Pace paceOf(List<Booking> completed) {
        List<Double> ratios = new ArrayList<>();
        for (Booking booking : completed) {
            BookingTracking tracking = booking.getTracking();
            if (tracking == null || tracking.getServiceStartedAt() == null || booking.getCompletedAt() == null) {
                continue;
            }
            int planned = durationOf(booking);
            double actual = Duration.between(tracking.getServiceStartedAt(), booking.getCompletedAt()).toSeconds() / 60.0;
            // Very short or very long records are mistakes (forgot to press start or complete).
            if (planned <= 0 || actual < planned * 0.3 || actual > planned * 3) {
                continue;
            }
            ratios.add(actual / planned);
        }
        if (ratios.size() < MIN_PACE_SAMPLES) {
            return new Pace(1.0, ratios.size());
        }
        ratios.sort(Double::compare);
        int middle = ratios.size() / 2;
        double median = ratios.size() % 2 == 1
                ? ratios.get(middle)
                : (ratios.get(middle - 1) + ratios.get(middle)) / 2;
        return new Pace(Math.min(MAX_PACE, Math.max(MIN_PACE, median)), ratios.size());
    }

    private static boolean isBefore(Booking other, Booking target) {
        int byTime = other.getBookingTime().compareTo(target.getBookingTime());
        return byTime < 0 || (byTime == 0 && other.getId() < target.getId());
    }

    private static LocalDateTime runningEnd(Booking running, LocalDateTime now) {
        BookingTracking tracking = running.getTracking();
        LocalDateTime end = tracking.getExpectedEndAt();
        if (end == null) {
            LocalDateTime started = tracking.getServiceStartedAt() == null ? now : tracking.getServiceStartedAt();
            end = started.plusMinutes(durationOf(running));
        }
        return end.isAfter(now) ? end : now.plus(OVERRUN_ALLOWANCE);
    }

    /** Late past the no-show grace and not on the way or checked in: the employee will not wait. */
    private static boolean likelyNoShow(Booking booking, LocalDateTime now) {
        JourneyStatus journey = booking.getTracking().journeyStatusOrDefault();
        return journey == JourneyStatus.NOT_STARTED && now.isAfter(BookingTimePolicy.noShowAllowedFrom(booking));
    }

    /** A client who shares the trip cannot start before they arrive. */
    private static LocalDateTime expectedArrival(Booking booking) {
        BookingTracking tracking = booking.getTracking();
        if (tracking.journeyStatusOrDefault() != JourneyStatus.ON_THE_WAY
                || tracking.getEtaMinutes() == null || tracking.getCustomerLocationUpdatedAt() == null) {
            return null;
        }
        return tracking.getCustomerLocationUpdatedAt().plusMinutes(tracking.getEtaMinutes());
    }

    private static Confidence confidence(Pace pace, int clientsAhead) {
        int samples = pace == null ? 0 : pace.samples();
        if (samples < MIN_PACE_SAMPLES || clientsAhead >= 3) {
            return clientsAhead == 0 ? Confidence.MEDIUM : Confidence.LOW;
        }
        return samples >= 8 && clientsAhead <= 1 ? Confidence.HIGH : Confidence.MEDIUM;
    }

    static int durationOf(Booking booking) {
        Integer minutes = booking.getTotalDurationMinutes();
        return minutes == null || minutes <= 0 ? DEFAULT_DURATION_MINUTES : minutes;
    }

    private static LocalDateTime later(LocalDateTime first, LocalDateTime second) {
        if (second == null) {
            return first;
        }
        return second.isAfter(first) ? second : first;
    }
}
