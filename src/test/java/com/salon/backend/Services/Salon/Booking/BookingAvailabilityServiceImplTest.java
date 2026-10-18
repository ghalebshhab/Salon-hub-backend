package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.services.EmployeeServiceCapability;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.Employee.EmployeeScheduleAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingAvailabilityServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Amman");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-17T07:07:00Z"),
            ZONE
    );

    @Mock private SalonRepo salonRepo;
    @Mock private SalonServiceRepo salonServiceRepo;
    @Mock private UserRepo userRepo;
    @Mock private EmployeeServiceCapabilityRepo capabilityRepo;
    @Mock private EmployeeScheduleRepo scheduleRepo;
    @Mock private BookingRepo bookingRepo;
    @Mock private EmployeeScheduleAvailability scheduleAvailability;

    private BookingAvailabilityServiceImpl availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new BookingAvailabilityServiceImpl(
                salonRepo,
                salonServiceRepo,
                userRepo,
                capabilityRepo,
                scheduleRepo,
                bookingRepo,
                scheduleAvailability,
                CLOCK
        );
        lenient().when(scheduleAvailability.isSalonOpenFor(any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    Salon salon = invocation.getArgument(0);
                    LocalTime start = invocation.getArgument(1);
                    int duration = invocation.getArgument(2);
                    return !start.isBefore(salon.getOpeningTime())
                            && !start.plusMinutes(duration).isAfter(salon.getClosingTime());
                });
        lenient().when(scheduleAvailability.isEmployeeAvailable(
                nullable(com.salon.backend.Entities.salons.schedule.EmployeeSchedule.class),
                any(),
                any(),
                any(),
                anyInt()
        ))
                .thenReturn(true);
    }

    @Test
    void returnsFifteenMinuteSlotsWithOnlyFreeQualifiedEmployees() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        Salon salon = salon(LocalTime.of(9, 0), LocalTime.of(11, 0));
        SalonService service = service(20L, salon, 30);
        User requester = user(1L, UserRole.USER, null);
        User employeeOne = user(2L, UserRole.Employee, salon);
        User employeeTwo = user(3L, UserRole.Employee, salon);
        Booking conflict = acceptedBooking(30L, employeeOne, date, LocalTime.of(9, 15), 30);

        arrangeAvailability(salon, service, requester, List.of(employeeOne, employeeTwo));
        when(capabilityRepo.findByEmployeeIdIn(any())).thenReturn(List.of(
                capability(employeeOne, service),
                capability(employeeTwo, service)
        ));
        when(bookingRepo.findByAssignedEmployeeIdInAndBookingDateAndStatus(
                any(),
                any(),
                any()
        )).thenReturn(List.of(conflict));

        ApiResponse<BookingAvailabilityResponse> response = availabilityService.getAvailability(
                new BookingAvailabilityRequest(10L, List.of(20L), date, null),
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals(7, response.getData().slots().size());
        assertEquals(LocalTime.of(9, 0), response.getData().slots().get(0).time());
        assertEquals(
                List.of(3L),
                response.getData().slots().get(0).eligibleEmployees().stream()
                        .map(employee -> employee.id())
                        .toList()
        );
        assertEquals(
                List.of(2L, 3L),
                response.getData().slots().get(3).eligibleEmployees().stream()
                        .map(employee -> employee.id())
                        .toList()
        );
        assertEquals(15, response.getData().slotIntervalMinutes());
        assertEquals(30, response.getData().minimumLeadMinutes());
        verify(bookingRepo).findByAssignedEmployeeIdInAndBookingDateAndStatus(
                any(),
                org.mockito.ArgumentMatchers.eq(date),
                org.mockito.ArgumentMatchers.eq(BookingStatus.ACCEPTED)
        );
    }

    @Test
    void sameDayAvailabilityStartsAfterTheThirtyMinuteLeadTime() {
        LocalDate today = LocalDate.of(2026, 10, 17);
        Salon salon = salon(LocalTime.of(10, 0), LocalTime.of(12, 0));
        SalonService service = service(20L, salon, 30);
        User requester = user(1L, UserRole.USER, null);
        User employee = user(2L, UserRole.Employee, salon);

        arrangeAvailability(salon, service, requester, List.of(employee));
        when(capabilityRepo.findByEmployeeIdIn(any()))
                .thenReturn(List.of(capability(employee, service)));
        when(bookingRepo.findByAssignedEmployeeIdInAndBookingDateAndStatus(any(), any(), any()))
                .thenReturn(List.of());

        ApiResponse<BookingAvailabilityResponse> response = availabilityService.getAvailability(
                new BookingAvailabilityRequest(10L, List.of(20L), today, null),
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals(LocalTime.of(10, 45), response.getData().slots().get(0).time());
    }

    @Test
    void selectedEmployeeMustBeQualifiedForEveryService() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        Salon salon = salon(LocalTime.of(9, 0), LocalTime.of(11, 0));
        SalonService service = service(20L, salon, 30);
        User requester = user(1L, UserRole.USER, null);
        User employee = user(2L, UserRole.Employee, salon);

        arrangeAvailability(salon, service, requester, List.of(employee));
        when(capabilityRepo.findByEmployeeIdIn(any())).thenReturn(List.of());

        ApiResponse<BookingAvailabilityResponse> response = availabilityService.getAvailability(
                new BookingAvailabilityRequest(10L, List.of(20L), date, 2L),
                1L
        );

        assertFalse(response.isSuccess());
        assertEquals(
                "Selected employee cannot perform every requested service",
                response.getMessage()
        );
    }

    @Test
    void selectedEmployeeIsMatchedByIdValue() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        Salon salon = salon(LocalTime.of(9, 0), LocalTime.of(11, 0));
        SalonService service = service(20L, salon, 30);
        User requester = user(1L, UserRole.USER, null);
        User employee = user(1_000L, UserRole.Employee, salon);

        arrangeAvailability(salon, service, requester, List.of(employee));
        when(capabilityRepo.findByEmployeeIdIn(any()))
                .thenReturn(List.of(capability(employee, service)));
        when(bookingRepo.findByAssignedEmployeeIdInAndBookingDateAndStatus(any(), any(), any()))
                .thenReturn(List.of());

        ApiResponse<BookingAvailabilityResponse> response = availabilityService.getAvailability(
                new BookingAvailabilityRequest(10L, List.of(20L), date, Long.valueOf(1_000L)),
                1L
        );

        assertTrue(response.isSuccess());
        assertFalse(response.getData().slots().isEmpty());
        assertEquals(1_000L, response.getData().slots().get(0).eligibleEmployees().get(0).id());
    }

    @Test
    void directSlotValidationRejectsTimesOutsideTheFifteenMinuteGrid() {
        Salon salon = salon(LocalTime.of(9, 0), LocalTime.of(11, 0));

        boolean available = availabilityService.isSlotAvailable(
                salon,
                java.util.Set.of(20L),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(9, 10),
                30,
                null
        );

        assertFalse(available);
    }

    private void arrangeAvailability(
            Salon salon,
            SalonService service,
            User requester,
            List<User> employees) {
        when(userRepo.findById(1L)).thenReturn(Optional.of(requester));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L), 10L))
                .thenReturn(List.of(service));
        when(userRepo.findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
                10L,
                UserRole.Employee,
                UserStatus.Active
        )).thenReturn(employees);
        lenient().when(scheduleRepo.findByEmployeeIdIn(any())).thenReturn(List.of());
    }

    private Salon salon(LocalTime openingTime, LocalTime closingTime) {
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Salon Test");
        salon.setStatus(SalonStatus.Accepted);
        salon.setOpeningTime(openingTime);
        salon.setClosingTime(closingTime);
        return salon;
    }

    private SalonService service(Long id, Salon salon, int durationMinutes) {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        return new SalonService(
                id,
                salon,
                "Haircut",
                null,
                BigDecimal.TEN,
                durationMinutes,
                true,
                now,
                now
        );
    }

    private User user(Long id, UserRole role, Salon salon) {
        User user = new User();
        user.setId(id);
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        user.setUserName("user" + id);
        user.setRole(role);
        user.setStatus(UserStatus.Active);
        user.setSalon(salon);
        return user;
    }

    private EmployeeServiceCapability capability(User employee, SalonService service) {
        EmployeeServiceCapability capability = new EmployeeServiceCapability();
        capability.setEmployee(employee);
        capability.setService(service);
        return capability;
    }

    private Booking acceptedBooking(
            Long id,
            User employee,
            LocalDate date,
            LocalTime time,
            int durationMinutes) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setAssignedEmployee(employee);
        booking.setBookingDate(date);
        booking.setBookingTime(time);
        booking.setTotalDurationMinutes(durationMinutes);
        booking.setStatus(BookingStatus.ACCEPTED);
        return booking;
    }
}
