package com.salon.backend.Services.Salon.Booking.Departure;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Departure.DeparturePlanResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.BookingTracking;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import com.salon.backend.Services.Salon.Booking.BookingTimePolicy;
import com.salon.backend.Services.Salon.Booking.Tracking.TravelTimeEstimator;
import com.salon.backend.Services.Salon.Booking.Tracking.TravelTimeEstimator.RouteEstimate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the "Leave on time" plan:
 * <ol>
 *   <li>{@link QueueForecaster} gives when the employee is really free.</li>
 *   <li>arriveBy = that time minus {@link #ARRIVAL_BUFFER}.</li>
 *   <li>The drive time with traffic comes from {@link TravelTimeEstimator}: first
 *       for a departure now, then again for the departure time it gives, so the
 *       predicted traffic at that hour is used.</li>
 *   <li>leaveAt = arriveBy minus the drive time.</li>
 * </ol>
 * Nothing is stored except the place the customer leaves from, so every read
 * follows the live queue and the traffic of that moment.
 */
@Service
@RequiredArgsConstructor
public class DeparturePlanServiceImpl implements DeparturePlanService {

    /** Time to check in before the turn. */
    static final Duration ARRIVAL_BUFFER = Duration.ofMinutes(5);
    static final int LEAVE_SOON_MINUTES = 15;
    /** After the leave time, "leave now" holds this long before it becomes "running late". */
    static final int LEAVE_NOW_GRACE_MINUTES = 5;
    /** The salon delay alert starts at this delay. */
    static final int DELAY_ALERT_MINUTES = 15;
    /** The salon delay alert is sent only this close to the turn. */
    static final Duration DELAY_ALERT_WINDOW = Duration.ofHours(4);
    /** A second traffic lookup for the departure time only when it is this far away. */
    static final Duration SECOND_PASS_FROM = Duration.ofMinutes(5);

    private static final List<BookingStatus> QUEUE_STATUSES = List.of(BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);

    private final BookingRepo bookingRepo;
    private final TravelTimeEstimator travelTimeEstimator;
    private final BookingNotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DeparturePlanResponse> getPlan(Long bookingId, Long customerId) {
        Booking booking = bookingId == null ? null : bookingRepo.findById(bookingId).orElse(null);
        if (!ownedBy(booking, customerId)) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        return ApiResponse.success("Departure plan fetched successfully", build(booking, now()));
    }

    @Override
    @Transactional
    public ApiResponse<DeparturePlanResponse> setOrigin(Long bookingId, CustomerPositionRequest origin, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        LocalDateTime now = now();
        if (booking.getStatus() != BookingStatus.ACCEPTED || booking.getBookingDate().isBefore(now.toLocalDate())) {
            return ApiResponse.error("Leave alerts are available only for an upcoming confirmed appointment");
        }
        if (booking.getTracking().journeyStatusOrDefault() != JourneyStatus.NOT_STARTED) {
            return ApiResponse.error("You are already on the way or at the salon");
        }
        String error = validate(origin);
        if (error != null) {
            return ApiResponse.error(error);
        }
        BookingTracking tracking = booking.getTracking();
        tracking.setPlanLatitude(origin.latitude());
        tracking.setPlanLongitude(origin.longitude());
        tracking.setPlanUpdatedAt(now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        return ApiResponse.success("We will tell you when to leave", build(booking, now));
    }

    @Override
    @Transactional
    public ApiResponse<DeparturePlanResponse> clearOrigin(Long bookingId, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        LocalDateTime now = now();
        if (booking.getTracking().hasDepartureOrigin()) {
            booking.getTracking().clearDepartureOrigin();
            booking.setUpdatedAt(now);
            bookingRepo.save(booking);
        }
        return ApiResponse.success("Leave alerts are off and your place is forgotten", build(booking, now));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAlertCandidateIds() {
        return bookingRepo.findDepartureAlertCandidateIds(BookingStatus.ACCEPTED, LocalDate.now(clock));
    }

    @Override
    @Transactional
    public void sendDueAlerts(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.ACCEPTED
                || !booking.getTracking().hasDepartureOrigin()) {
            return;
        }
        LocalDateTime now = now();
        if (now.isAfter(BookingTimePolicy.noShowAllowedFrom(booking))) {
            return;
        }
        DeparturePlanResponse response = build(booking, now);
        switch (response.phase()) {
            case "LEAVE_SOON" -> notificationService.departureLeaveSoon(
                    booking, response.minutesUntilLeave(), response.travelMinutes(), response.expectedStartAt());
            case "LEAVE_NOW", "RUNNING_LATE" -> notificationService.departureLeaveNow(
                    booking, response.travelMinutes(), now.plusMinutes(response.travelMinutes()),
                    response.expectedStartAt(), "RUNNING_LATE".equals(response.phase()));
            default -> {
                // Nothing to send yet.
            }
        }
        boolean waiting = "WAIT".equals(response.phase()) || "LEAVE_SOON".equals(response.phase());
        boolean close = !response.slotStart().isAfter(now.plus(DELAY_ALERT_WINDOW));
        if (waiting && close && response.delayMinutes() >= DELAY_ALERT_MINUTES) {
            notificationService.salonRunningLate(booking, response.delayMinutes(), response.expectedStartAt());
        }
    }

    // ---------- plan ----------

    private DeparturePlanResponse build(Booking booking, LocalDateTime now) {
        BookingTracking tracking = booking.getTracking();
        JourneyStatus journey = tracking.journeyStatusOrDefault();
        LocalDateTime slot = BookingTimePolicy.appointmentStart(booking);
        boolean alertsOn = tracking.hasDepartureOrigin();

        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            LocalDateTime started = tracking.getServiceStartedAt() == null ? slot : tracking.getServiceStartedAt();
            return plan(booking, "IN_SERVICE", slot, started, 0, 0, "HIGH", null, null, null, alertsOn, now);
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED || booking.getBookingDate().isBefore(now.toLocalDate())) {
            return plan(booking, "CLOSED", slot, slot, 0, 0, "SCHEDULED", null, null, null, false, now);
        }

        QueueForecaster.Forecast forecast = forecast(booking, now);
        LocalDateTime expectedStart = forecast.expectedStart();
        LocalDateTime arriveBy = expectedStart.minus(ARRIVAL_BUFFER);
        String confidence = forecast.confidence().name();

        if (journey == JourneyStatus.ARRIVED || journey == JourneyStatus.ON_THE_WAY) {
            return plan(booking, journey.name(), slot, expectedStart, forecast.delayMinutes(),
                    forecast.clientsAhead(), confidence, null, arriveBy, null, alertsOn, now);
        }

        RouteEstimate route = null;
        LocalDateTime leaveAt = null;
        Salon salon = booking.getSalon();
        if (alertsOn && salon.getLatitude() != null && salon.getLongitude() != null) {
            route = travel(tracking, salon, null);
            leaveAt = arriveBy.minusMinutes(route.minutes());
            if (leaveAt.isAfter(now.plus(SECOND_PASS_FROM))) {
                // Predicted traffic at the hour the customer will really drive.
                route = travel(tracking, salon, leaveAt);
                leaveAt = arriveBy.minusMinutes(route.minutes());
            }
        }

        String phase;
        if (booking.getBookingDate().isAfter(now.toLocalDate())) {
            phase = "PLANNED_DAY";
        } else if (leaveAt == null) {
            phase = "WAIT";
        } else {
            long untilLeave = minutesUntil(now, leaveAt);
            if (untilLeave > LEAVE_SOON_MINUTES) {
                phase = "WAIT";
            } else if (untilLeave > 0) {
                phase = "LEAVE_SOON";
            } else if (untilLeave >= -LEAVE_NOW_GRACE_MINUTES) {
                phase = "LEAVE_NOW";
            } else {
                phase = "RUNNING_LATE";
            }
        }
        return plan(booking, phase, slot, expectedStart, forecast.delayMinutes(), forecast.clientsAhead(),
                confidence, route, arriveBy, leaveAt, alertsOn, now);
    }

    private QueueForecaster.Forecast forecast(Booking booking, LocalDateTime now) {
        if (booking.getAssignedEmployee() == null) {
            return QueueForecaster.forecast(booking, List.of(), QueueForecaster.Pace.UNKNOWN, now);
        }
        Long employeeId = booking.getAssignedEmployee().getId();
        List<Booking> sameDay = bookingRepo.findByAssignedEmployeeIdAndBookingDateAndStatusIn(
                employeeId, booking.getBookingDate(), QUEUE_STATUSES);
        QueueForecaster.Pace pace = QueueForecaster.paceOf(
                bookingRepo.findTop20ByAssignedEmployeeIdAndStatusAndTrackingServiceStartedAtIsNotNullOrderByCompletedAtDesc(
                        employeeId, BookingStatus.COMPLETED));
        return QueueForecaster.forecast(booking, sameDay, pace, now);
    }

    private RouteEstimate travel(BookingTracking tracking, Salon salon, LocalDateTime departAt) {
        return travelTimeEstimator.route(tracking.getPlanLatitude(), tracking.getPlanLongitude(),
                salon.getLatitude(), salon.getLongitude(), departAt);
    }

    private DeparturePlanResponse plan(Booking booking, String phase, LocalDateTime slot, LocalDateTime expectedStart,
                      int delayMinutes, int clientsAhead, String confidence, RouteEstimate route,
                      LocalDateTime arriveBy, LocalDateTime leaveAt, boolean alertsOn, LocalDateTime now) {
        BookingTracking tracking = booking.getTracking();
        return new DeparturePlanResponse(
                booking.getId(),
                phase,
                booking.getSalon().getName(),
                slot,
                expectedStart,
                delayMinutes,
                clientsAhead,
                confidence,
                route == null ? null : route.minutes(),
                route == null ? null : route.freeFlowMinutes(),
                route == null ? null : route.trafficDelayMinutes(),
                route == null ? null : route.trafficLevel(),
                route == null ? null : route.distanceMeters(),
                route == null ? null : route.source(),
                (int) ARRIVAL_BUFFER.toMinutes(),
                arriveBy,
                leaveAt,
                leaveAt == null ? null : (int) minutesUntil(now, leaveAt),
                alertsOn,
                alertsOn ? tracking.getPlanUpdatedAt() : null,
                now
        );
    }

    /** Whole minutes from now to the time, rounded up while in the future. */
    private static long minutesUntil(LocalDateTime now, LocalDateTime time) {
        long seconds = Duration.between(now, time).toSeconds();
        return seconds > 0 ? (seconds + 59) / 60 : -((-seconds) / 60);
    }

    // ---------- access and input ----------

    private boolean ownedBy(Booking booking, Long customerId) {
        return booking != null && customerId != null && booking.getCustomer().getId() == customerId;
    }

    private Booking lockForCustomer(Long bookingId, Long customerId) {
        if (bookingId == null || customerId == null) {
            return null;
        }
        Booking booking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        return ownedBy(booking, customerId) ? booking : null;
    }

    private static String validate(CustomerPositionRequest origin) {
        if (origin == null || origin.latitude() == null || origin.longitude() == null) {
            return "Your place is required";
        }
        double lat = origin.latitude();
        double lng = origin.longitude();
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return "Your place is not valid";
        }
        return null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
