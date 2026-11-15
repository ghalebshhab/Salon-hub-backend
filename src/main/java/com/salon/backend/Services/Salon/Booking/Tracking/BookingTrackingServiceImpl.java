package com.salon.backend.Services.Salon.Booking.Tracking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.BookingTrackingResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.DTOs.Salon.Booking.Tracking.ExtendServiceRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.BookingTracking;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import com.salon.backend.Services.Salon.Booking.BookingResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingTrackingServiceImpl implements BookingTrackingService {

    /** The customer can share the trip from this long before the appointment. */
    static final Duration JOURNEY_WINDOW_BEFORE = Duration.ofHours(3);
    /** The employee can start the service this early (customer came early). */
    static final Duration EARLY_START = Duration.ofMinutes(60);
    /** Within this distance the customer counts as arrived. */
    static final int ARRIVAL_RADIUS_METERS = 80;
    /** Position updates closer together than this are ignored. */
    static final Duration MIN_UPDATE_INTERVAL = Duration.ofSeconds(8);
    static final int MAX_EXTENSION_STEP = 60;
    static final int MAX_TOTAL_OVERRUN = 120;
    static final String ETA_SOURCE = "DISTANCE_ESTIMATE";

    private static final String CUSTOMER = "CUSTOMER";
    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String OWNER = "OWNER";

    private final BookingRepo bookingRepo;
    private final TravelTimeEstimator travelTimeEstimator;
    private final BookingNotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BookingTrackingResponse> getTracking(Long bookingId, Long viewerId) {
        Booking booking = bookingId == null ? null : bookingRepo.findById(bookingId).orElse(null);
        String role = roleOf(booking, viewerId);
        if (role == null) {
            return ApiResponse.error("Booking was not found");
        }
        return ApiResponse.success("Booking tracking fetched successfully", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> startJourney(
            Long bookingId, CustomerPositionRequest position, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        String windowError = journeyWindowError(booking);
        if (windowError != null) {
            return ApiResponse.error(windowError);
        }
        BookingTracking tracking = booking.getTracking();
        if (tracking.journeyStatusOrDefault() == JourneyStatus.ARRIVED) {
            return ApiResponse.error("You are already marked as arrived");
        }
        String positionError = validatePosition(position, false);
        if (positionError != null) {
            return ApiResponse.error(positionError);
        }

        LocalDateTime now = now();
        tracking.setJourneyStatus(JourneyStatus.ON_THE_WAY);
        tracking.setJourneyStartedAt(now);
        boolean arrived = hasPosition(position) && applyPosition(booking, position, now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        if (arrived) {
            notificationService.customerArrived(booking);
        } else {
            notificationService.customerOnTheWay(booking);
        }
        return ApiResponse.success("Trip sharing started", toResponse(booking, CUSTOMER));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> updatePosition(
            Long bookingId, CustomerPositionRequest position, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        BookingTracking tracking = booking.getTracking();
        if (booking.getStatus() != BookingStatus.ACCEPTED
                || tracking.journeyStatusOrDefault() != JourneyStatus.ON_THE_WAY) {
            return ApiResponse.error("Trip sharing is not active for this booking");
        }
        String positionError = validatePosition(position, true);
        if (positionError != null) {
            return ApiResponse.error(positionError);
        }

        LocalDateTime now = now();
        LocalDateTime last = tracking.getCustomerLocationUpdatedAt();
        if (last != null && Duration.between(last, now).compareTo(MIN_UPDATE_INTERVAL) < 0) {
            return ApiResponse.success("Position update skipped", toResponse(booking, CUSTOMER));
        }
        boolean arrived = applyPosition(booking, position, now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        if (arrived) {
            notificationService.customerArrived(booking);
        }
        return ApiResponse.success("Position updated", toResponse(booking, CUSTOMER));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> markArrived(Long bookingId, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        String windowError = journeyWindowError(booking);
        if (windowError != null) {
            return ApiResponse.error(windowError);
        }
        LocalDateTime now = now();
        markArrived(booking, now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        notificationService.customerArrived(booking);
        return ApiResponse.success("Arrival shared with the salon", toResponse(booking, CUSTOMER));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> stopJourney(Long bookingId, Long customerId) {
        Booking booking = lockForCustomer(bookingId, customerId);
        if (booking == null) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        BookingTracking tracking = booking.getTracking();
        if (tracking.journeyStatusOrDefault() == JourneyStatus.ON_THE_WAY) {
            tracking.setJourneyStatus(JourneyStatus.NOT_STARTED);
            tracking.setJourneyStartedAt(null);
            tracking.setCustomerLocationUpdatedAt(null);
            tracking.clearCustomerPosition();
            booking.setUpdatedAt(now());
            bookingRepo.save(booking);
        }
        return ApiResponse.success("Trip sharing stopped", toResponse(booking, CUSTOMER));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> startService(Long bookingId, Long staffId) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        String role = staffRole(booking, staffId);
        if (role == null) {
            return ApiResponse.error("Booking assigned to you was not found");
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            return ApiResponse.error("Only a confirmed appointment can be started");
        }
        if (booking.getAssignedEmployee() == null) {
            return ApiResponse.error("Assign an employee before starting the service");
        }
        LocalDateTime now = now();
        if (!booking.getBookingDate().equals(now.toLocalDate())) {
            return ApiResponse.error("The service can be started only on the appointment day");
        }
        if (now.isBefore(appointmentStart(booking).minus(EARLY_START))) {
            return ApiResponse.error("The service can be started at most "
                    + EARLY_START.toMinutes() + " minutes before the appointment");
        }
        Booking running = bookingRepo.findFirstByAssignedEmployeeIdAndStatusOrderByBookingTimeAsc(
                booking.getAssignedEmployee().getId(), BookingStatus.IN_PROGRESS).orElse(null);
        if (running != null && !running.getId().equals(booking.getId())) {
            return ApiResponse.error("Finish the current client's service before starting another one");
        }

        BookingTracking tracking = booking.getTracking();
        if (tracking.journeyStatusOrDefault() != JourneyStatus.ARRIVED) {
            markArrived(booking, now);
        }
        booking.setStatus(BookingStatus.IN_PROGRESS);
        tracking.setServiceStartedAt(now);
        tracking.setExpectedEndAt(now.plusMinutes(booking.getTotalDurationMinutes()));
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        notificationService.serviceStarted(booking);
        return ApiResponse.success("Service started", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> extendService(
            Long bookingId, ExtendServiceRequest request, Long staffId) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        String role = staffRole(booking, staffId);
        if (role == null) {
            return ApiResponse.error("Booking assigned to you was not found");
        }
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            return ApiResponse.error("Only a running service can be extended");
        }
        Integer minutes = request == null ? null : request.minutes();
        if (minutes == null || minutes < 5 || minutes > MAX_EXTENSION_STEP) {
            return ApiResponse.error("Extend by 5 to " + MAX_EXTENSION_STEP + " minutes");
        }
        BookingTracking tracking = booking.getTracking();
        LocalDateTime newEnd = tracking.getExpectedEndAt().plusMinutes(minutes);
        LocalDateTime plannedEnd = tracking.getServiceStartedAt().plusMinutes(booking.getTotalDurationMinutes());
        if (Duration.between(plannedEnd, newEnd).toMinutes() > MAX_TOTAL_OVERRUN) {
            return ApiResponse.error("A service cannot run more than " + MAX_TOTAL_OVERRUN + " minutes over its plan");
        }
        tracking.setExpectedEndAt(newEnd);
        booking.setUpdatedAt(now());
        bookingRepo.save(booking);
        return ApiResponse.success("Expected end time updated", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingTrackingResponse> completeService(Long bookingId, Long staffId) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        String role = staffRole(booking, staffId);
        if (role == null) {
            return ApiResponse.error("Booking assigned to you was not found");
        }
        if (booking.getStatus() != BookingStatus.IN_PROGRESS && booking.getStatus() != BookingStatus.ACCEPTED) {
            return ApiResponse.error("Only a confirmed or running appointment can be completed");
        }
        LocalDateTime now = now();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        booking.setUpdatedAt(now);
        booking.getTracking().clearCustomerPosition();
        bookingRepo.save(booking);
        notificationService.bookingCompleted(booking);
        return ApiResponse.success("Service completed", toResponse(booking, role));
    }

    // ---------- rules ----------

    private String journeyWindowError(Booking booking) {
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            return "Trip sharing is available only for a confirmed appointment";
        }
        LocalDateTime now = now();
        LocalDateTime start = appointmentStart(booking);
        if (now.isBefore(start.minus(JOURNEY_WINDOW_BEFORE))) {
            return "You can share your trip from " + JOURNEY_WINDOW_BEFORE.toHours()
                    + " hours before the appointment";
        }
        if (now.isAfter(start.plusMinutes(booking.getTotalDurationMinutes()))) {
            return "This appointment time has passed";
        }
        return null;
    }

    private String validatePosition(CustomerPositionRequest position, boolean required) {
        boolean has = hasPosition(position);
        if (!has) {
            boolean partial = position != null && (position.latitude() != null || position.longitude() != null);
            if (partial) {
                return "Latitude and longitude must be sent together";
            }
            return required ? "Your position is required" : null;
        }
        double lat = position.latitude();
        double lng = position.longitude();
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return "Your position is not valid";
        }
        return null;
    }

    private boolean hasPosition(CustomerPositionRequest position) {
        return position != null && position.latitude() != null && position.longitude() != null;
    }

    /** Stores the position and estimate. Returns true when this update counts as arrival. */
    private boolean applyPosition(Booking booking, CustomerPositionRequest position, LocalDateTime now) {
        BookingTracking tracking = booking.getTracking();
        Salon salon = booking.getSalon();
        tracking.setCustomerLatitude(position.latitude());
        tracking.setCustomerLongitude(position.longitude());
        tracking.setCustomerLocationUpdatedAt(now);
        if (salon.getLatitude() == null || salon.getLongitude() == null) {
            tracking.setDistanceMeters(null);
            tracking.setEtaMinutes(null);
            return false;
        }
        TravelTimeEstimator.TravelEstimate estimate = travelTimeEstimator.estimate(
                position.latitude(), position.longitude(), salon.getLatitude(), salon.getLongitude());
        tracking.setDistanceMeters(estimate.distanceMeters());
        tracking.setEtaMinutes(estimate.etaMinutes());
        if (estimate.distanceMeters() <= ARRIVAL_RADIUS_METERS) {
            markArrived(booking, now);
            return true;
        }
        return false;
    }

    private void markArrived(Booking booking, LocalDateTime now) {
        BookingTracking tracking = booking.getTracking();
        tracking.setJourneyStatus(JourneyStatus.ARRIVED);
        if (tracking.getArrivedAt() == null) {
            tracking.setArrivedAt(now);
        }
        tracking.clearCustomerPosition();
    }

    // ---------- access ----------

    private Booking lockForCustomer(Long bookingId, Long customerId) {
        if (bookingId == null || customerId == null) {
            return null;
        }
        Booking booking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        return booking != null && booking.getCustomer().getId() == customerId ? booking : null;
    }

    private String roleOf(Booking booking, Long viewerId) {
        if (booking == null || viewerId == null) {
            return null;
        }
        if (booking.getCustomer().getId() == viewerId) {
            return CUSTOMER;
        }
        return staffRole(booking, viewerId);
    }

    private String staffRole(Booking booking, Long staffId) {
        if (booking == null || staffId == null) {
            return null;
        }
        User employee = booking.getAssignedEmployee();
        if (employee != null && employee.getId() == staffId) {
            return EMPLOYEE;
        }
        User owner = booking.getSalon().getOwner();
        if (owner != null && owner.getId() == staffId) {
            return OWNER;
        }
        return null;
    }

    // ---------- response ----------

    private BookingTrackingResponse toResponse(Booking booking, String role) {
        LocalDateTime now = now();
        BookingTracking tracking = booking.getTracking();
        JourneyStatus journey = tracking.journeyStatusOrDefault();
        boolean staff = !CUSTOMER.equals(role);
        boolean showPosition = staff && journey == JourneyStatus.ON_THE_WAY;
        User employee = booking.getAssignedEmployee();

        String employeeState;
        LocalDateTime busyUntil = null;
        if (employee == null) {
            employeeState = "AWAITING_EMPLOYEE";
        } else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            employeeState = "SERVING_YOU";
        } else if (booking.getStatus() == BookingStatus.COMPLETED) {
            employeeState = "FINISHED";
        } else {
            Booking running = bookingRepo.findFirstByAssignedEmployeeIdAndStatusOrderByBookingTimeAsc(
                    employee.getId(), BookingStatus.IN_PROGRESS).orElse(null);
            if (running != null && !running.getId().equals(booking.getId())) {
                employeeState = "BUSY_WITH_ANOTHER_CLIENT";
                busyUntil = running.getTracking().getExpectedEndAt();
            } else {
                employeeState = "AVAILABLE";
            }
        }

        Integer remaining = null;
        Integer progress = null;
        if (booking.getStatus() == BookingStatus.IN_PROGRESS
                && tracking.getServiceStartedAt() != null && tracking.getExpectedEndAt() != null) {
            long total = Math.max(1, Duration.between(tracking.getServiceStartedAt(), tracking.getExpectedEndAt()).toSeconds());
            long done = Math.max(0, Duration.between(tracking.getServiceStartedAt(), now).toSeconds());
            remaining = (int) Math.max(0, Math.ceil(Duration.between(now, tracking.getExpectedEndAt()).toSeconds() / 60.0));
            progress = (int) Math.min(100, Math.round(done * 100.0 / total));
        }

        LocalDateTime expectedArrival = tracking.getEtaMinutes() != null && tracking.getCustomerLocationUpdatedAt() != null
                ? tracking.getCustomerLocationUpdatedAt().plusMinutes(tracking.getEtaMinutes())
                : null;
        boolean today = booking.getBookingDate().equals(LocalDate.now(clock));

        return new BookingTrackingResponse(
                booking.getId(),
                role,
                booking.getStatus(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTotalDurationMinutes(),
                booking.getSalon().getName(),
                booking.getSalon().getLatitude(),
                booking.getSalon().getLongitude(),
                BookingResponseMapper.fullName(booking.getCustomer()),
                staff ? booking.getCustomerPhoneNumber() : null,
                employee == null ? null : BookingResponseMapper.fullName(employee),
                journey,
                tracking.getJourneyStartedAt(),
                journey == JourneyStatus.ON_THE_WAY ? tracking.getDistanceMeters() : null,
                journey == JourneyStatus.ON_THE_WAY ? tracking.getEtaMinutes() : null,
                journey == JourneyStatus.ON_THE_WAY ? expectedArrival : null,
                tracking.getCustomerLocationUpdatedAt(),
                tracking.getArrivedAt(),
                showPosition ? tracking.getCustomerLatitude() : null,
                showPosition ? tracking.getCustomerLongitude() : null,
                ETA_SOURCE,
                employeeState,
                busyUntil,
                tracking.getServiceStartedAt(),
                tracking.getExpectedEndAt(),
                remaining,
                progress,
                booking.getCompletedAt(),
                CUSTOMER.equals(role) && journeyWindowError(booking) == null && journey != JourneyStatus.ARRIVED,
                staff && booking.getStatus() == BookingStatus.ACCEPTED && today && employee != null
                        && !now.isBefore(appointmentStart(booking).minus(EARLY_START)),
                staff && (booking.getStatus() == BookingStatus.IN_PROGRESS
                        || booking.getStatus() == BookingStatus.ACCEPTED),
                now
        );
    }

    private LocalDateTime appointmentStart(Booking booking) {
        return LocalDateTime.of(booking.getBookingDate(), booking.getBookingTime());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
