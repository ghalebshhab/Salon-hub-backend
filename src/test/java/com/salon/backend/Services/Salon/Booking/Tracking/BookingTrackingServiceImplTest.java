package com.salon.backend.Services.Salon.Booking.Tracking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.BookingTrackingResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.DTOs.Salon.Booking.Tracking.ExtendServiceRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingTrackingServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Amman");
    /** 10:00 local time on 2026-10-17. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T07:00:00Z"), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final double SALON_LAT = 31.9508;
    private static final double SALON_LNG = 35.9241;

    @Mock private BookingRepo bookingRepo;
    @Mock private BookingNotificationService notificationService;

    private BookingTrackingServiceImpl service;
    private User customer;
    private User employee;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new BookingTrackingServiceImpl(
                bookingRepo, new DistanceTravelTimeEstimator(), notificationService, CLOCK);
        customer = user(1L, "Lina");
        employee = user(2L, "Sara");
        owner = user(3L, "Omar");
        lenient().when(bookingRepo.findFirstByAssignedEmployeeIdAndStatusOrderByBookingTimeAsc(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void customerStartsTheTripAndTheEmployeeSeesDistanceEtaAndPosition() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingTrackingResponse> started = service.startJourney(
                30L, new CustomerPositionRequest(31.98, 35.87), 1L);

        assertTrue(started.isSuccess());
        assertEquals(JourneyStatus.ON_THE_WAY, started.getData().journeyStatus());
        assertNotNull(started.getData().etaMinutes());
        assertTrue(started.getData().distanceMeters() > 5000);
        assertNull(started.getData().customerLatitude(), "The customer view never echoes coordinates");
        verify(notificationService).customerOnTheWay(booking);

        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));
        BookingTrackingResponse employeeView = service.getTracking(30L, 2L).getData();
        assertEquals("EMPLOYEE", employeeView.viewerRole());
        assertEquals(31.98, employeeView.customerLatitude());
        assertEquals("0790000000", employeeView.customerPhoneNumber());
    }

    @Test
    void aPositionNearTheSalonMarksArrivalAndClearsCoordinates() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        booking.getTracking().setJourneyStatus(JourneyStatus.ON_THE_WAY);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingTrackingResponse> response = service.updatePosition(
                30L, new CustomerPositionRequest(SALON_LAT + 0.0002, SALON_LNG), 1L);

        assertTrue(response.isSuccess());
        assertEquals(JourneyStatus.ARRIVED, booking.getTracking().getJourneyStatus());
        assertNull(booking.getTracking().getCustomerLatitude());
        verify(notificationService).customerArrived(booking);
    }

    @Test
    void tripSharingIsClosedMoreThanThreeHoursBeforeTheAppointment() {
        Booking booking = acceptedBooking(LocalTime.of(15, 0));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingTrackingResponse> response = service.startJourney(30L, null, 1L);

        assertFalse(response.isSuccess());
        assertEquals(JourneyStatus.NOT_STARTED, booking.getTracking().journeyStatusOrDefault());
    }

    @Test
    void anotherCustomerCannotShareATripForThisBooking() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingTrackingResponse> response = service.startJourney(30L, null, 99L);

        assertFalse(response.isSuccess());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void employeeStartsExtendsAndCompletesTheService() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        BookingTrackingResponse started = service.startService(30L, 2L).getData();
        assertEquals(BookingStatus.IN_PROGRESS, started.status());
        assertEquals("SERVING_YOU", started.employeeState());
        assertEquals(LocalDateTime.of(TODAY, LocalTime.of(10, 45)), started.expectedEndAt());
        assertEquals(JourneyStatus.ARRIVED, started.journeyStatus());
        verify(notificationService).serviceStarted(booking);

        BookingTrackingResponse extended = service.extendService(30L, new ExtendServiceRequest(10), 2L).getData();
        assertEquals(LocalDateTime.of(TODAY, LocalTime.of(10, 55)), extended.expectedEndAt());

        BookingTrackingResponse completed = service.completeService(30L, 2L).getData();
        assertEquals(BookingStatus.COMPLETED, completed.status());
        verify(notificationService).bookingCompleted(booking);
    }

    @Test
    void theServiceCannotStartMoreThanAnHourEarly() {
        Booking booking = acceptedBooking(LocalTime.of(11, 30));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertFalse(service.startService(30L, 2L).isSuccess());
        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
    }

    @Test
    void anEmployeeServingAnotherClientCannotStartASecondService() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        Booking running = acceptedBooking(LocalTime.of(10, 0));
        running.setId(31L);
        running.setStatus(BookingStatus.IN_PROGRESS);
        running.getTracking().setExpectedEndAt(LocalDateTime.of(TODAY, LocalTime.of(10, 40)));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(bookingRepo.findFirstByAssignedEmployeeIdAndStatusOrderByBookingTimeAsc(2L, BookingStatus.IN_PROGRESS))
                .thenReturn(Optional.of(running));

        ApiResponse<BookingTrackingResponse> response = service.startService(30L, 2L);
        assertFalse(response.isSuccess());

        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));
        BookingTrackingResponse customerView = service.getTracking(30L, 1L).getData();
        assertEquals("BUSY_WITH_ANOTHER_CLIENT", customerView.employeeState());
        assertEquals(LocalDateTime.of(TODAY, LocalTime.of(10, 40)), customerView.employeeBusyUntil());
        assertNull(customerView.customerPhoneNumber());
    }

    @Test
    void ownerCanCompleteButAStrangerCannotSeeTracking() {
        Booking booking = acceptedBooking(LocalTime.of(10, 30));
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        assertEquals("OWNER", service.getTracking(30L, 3L).getData().viewerRole());
        assertFalse(service.getTracking(30L, 99L).isSuccess());
    }

    private Booking acceptedBooking(LocalTime time) {
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Plum Studio");
        salon.setOwner(owner);
        salon.setLatitude(SALON_LAT);
        salon.setLongitude(SALON_LNG);

        Booking booking = new Booking();
        booking.setId(30L);
        booking.setSalon(salon);
        booking.setCustomer(customer);
        booking.setAssignedEmployee(employee);
        booking.setCustomerPhoneNumber("0790000000");
        booking.setBookingDate(TODAY);
        booking.setBookingTime(time);
        booking.setTotalDurationMinutes(45);
        booking.setStatus(BookingStatus.ACCEPTED);
        return booking;
    }

    private User user(long id, String firstName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setUserName(firstName.toLowerCase());
        return user;
    }
}
