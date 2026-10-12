package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeScheduleResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeWorkingPeriodRequest;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeScheduleRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeScheduleManagementImplTest {

    @Mock private EmployeeScheduleRepo scheduleRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @InjectMocks private EmployeeScheduleManagementImpl scheduleManagement;

    @Test
    void ownerCanConfigureSplitPeriodsThatRepresentABreak() {
        Salon salon = salon();
        User employee = employee(salon);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(scheduleRepo.findByEmployeeIdForUpdate(4L)).thenReturn(Optional.empty());

        ApiResponse<EmployeeScheduleResponse> response = scheduleManagement.updateEmployeeSchedule(
                4L,
                new UpdateEmployeeScheduleRequest(List.of(
                        period(DayOfWeek.MONDAY, 13, 0, 17, 0),
                        period(DayOfWeek.MONDAY, 9, 0, 12, 0)
                )),
                2L
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getData().configured());
        assertEquals(LocalTime.of(9, 0), response.getData().periods().get(0).startTime());
        assertEquals(LocalTime.of(13, 0), response.getData().periods().get(1).startTime());
        verify(scheduleRepo).save(any(EmployeeSchedule.class));
    }

    @Test
    void overlappingPeriodsAreRejected() {
        Salon salon = salon();
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(salon)));

        ApiResponse<EmployeeScheduleResponse> response = scheduleManagement.updateEmployeeSchedule(
                4L,
                new UpdateEmployeeScheduleRequest(List.of(
                        period(DayOfWeek.MONDAY, 9, 0, 13, 0),
                        period(DayOfWeek.MONDAY, 12, 0, 17, 0)
                )),
                2L
        );

        assertFalse(response.isSuccess());
        verify(scheduleRepo, never()).save(any());
    }

    @Test
    void periodsOutsideSalonHoursAreRejected() {
        Salon salon = salon();
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(salon)));

        ApiResponse<EmployeeScheduleResponse> response = scheduleManagement.updateEmployeeSchedule(
                4L,
                new UpdateEmployeeScheduleRequest(List.of(
                        period(DayOfWeek.TUESDAY, 8, 30, 12, 0)
                )),
                2L
        );

        assertFalse(response.isSuccess());
        verify(scheduleRepo, never()).save(any());
    }

    @Test
    void emptyConfiguredScheduleMakesEmployeeUnavailableAllWeek() {
        Salon salon = salon();
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(salon)));
        when(scheduleRepo.findByEmployeeIdForUpdate(4L)).thenReturn(Optional.empty());

        ApiResponse<EmployeeScheduleResponse> response = scheduleManagement.updateEmployeeSchedule(
                4L,
                new UpdateEmployeeScheduleRequest(List.of()),
                2L
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getData().configured());
        assertTrue(response.getData().periods().isEmpty());
    }

    private EmployeeWorkingPeriodRequest period(
            DayOfWeek day,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
        return new EmployeeWorkingPeriodRequest(
                day,
                LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute)
        );
    }

    private Salon salon() {
        User owner = new User();
        owner.setId(2L);

        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Salon");
        salon.setOwner(owner);
        salon.setOpeningTime(LocalTime.of(9, 0));
        salon.setClosingTime(LocalTime.of(18, 0));
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        return salon;
    }

    private User employee(Salon salon) {
        User employee = new User();
        employee.setId(4L);
        employee.setFirstName("Lina");
        employee.setLastName("Stylist");
        employee.setUserName("lina");
        employee.setRole(UserRole.Employee);
        employee.setStatus(UserStatus.Active);
        employee.setSalon(salon);
        return employee;
    }
}
