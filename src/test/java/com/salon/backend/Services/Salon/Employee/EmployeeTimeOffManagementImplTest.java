package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.EmployeeTimeOffRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTimeOffManagementImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T07:00:00Z"), ZoneId.of("Asia/Amman"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    @Mock private EmployeeTimeOffRepo timeOffRepo;
    @Mock private BookingRepo bookingRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;

    private EmployeeTimeOffManagementImpl service;
    private Salon salon;

    @BeforeEach
    void setUp() {
        service = new EmployeeTimeOffManagementImpl(timeOffRepo, bookingRepo, salonRepo, userRepo, CLOCK,
                org.mockito.Mockito.mock(com.salon.backend.Services.Salon.SalonAccess.class));
        salon = new Salon();
        salon.setId(10L);
        salon.setStatus(SalonStatus.Accepted);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
    }

    @Test
    void savesTimeOffAndReportsAppointmentsThatNeedAnotherEmployee() {
        when(userRepo.findById(4L)).thenReturn(Optional.of(employee(4L, salon)));
        when(bookingRepo.countByAssignedEmployeeIdAndBookingDateBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(2L);

        ApiResponse<EmployeeTimeOffResponse> response = service.addTimeOff(
                4L, new EmployeeTimeOffRequest(TODAY.plusDays(1), TODAY.plusDays(3), " Vacation "), 2L);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().affectedBookings());
        assertEquals("Vacation", response.getData().reason());
        verify(timeOffRepo).save(any());
    }

    @Test
    void rejectsAnEndDateBeforeTheStartDate() {
        when(userRepo.findById(4L)).thenReturn(Optional.of(employee(4L, salon)));

        ApiResponse<EmployeeTimeOffResponse> response = service.addTimeOff(
                4L, new EmployeeTimeOffRequest(TODAY.plusDays(3), TODAY.plusDays(1), null), 2L);

        assertFalse(response.isSuccess());
        verify(timeOffRepo, never()).save(any());
    }

    @Test
    void rejectsOverlappingTimeOff() {
        when(userRepo.findById(4L)).thenReturn(Optional.of(employee(4L, salon)));
        when(timeOffRepo.existsOverlapping(anyLong(), any(), any())).thenReturn(true);

        ApiResponse<EmployeeTimeOffResponse> response = service.addTimeOff(
                4L, new EmployeeTimeOffRequest(TODAY, TODAY, null), 2L);

        assertFalse(response.isSuccess());
        verify(timeOffRepo, never()).save(any());
    }

    @Test
    void ownerCannotAddTimeOffForAnotherSalonsEmployee() {
        Salon other = new Salon();
        other.setId(11L);
        when(userRepo.findById(4L)).thenReturn(Optional.of(employee(4L, other)));

        ApiResponse<EmployeeTimeOffResponse> response = service.addTimeOff(
                4L, new EmployeeTimeOffRequest(TODAY, TODAY, null), 2L);

        assertFalse(response.isSuccess());
        verify(timeOffRepo, never()).save(any());
    }

    private User employee(long id, Salon employeeSalon) {
        User user = new User();
        user.setId(id);
        user.setRole(UserRole.Employee);
        user.setSalon(employeeSalon);
        return user;
    }
}
