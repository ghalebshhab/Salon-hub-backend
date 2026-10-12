package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
import com.salon.backend.Entities.salons.schedule.EmployeeWorkingPeriod;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeScheduleAvailabilityTest {

    @Mock private EmployeeScheduleRepo scheduleRepo;
    @InjectMocks private EmployeeScheduleAvailability availability;

    @Test
    void unconfiguredEmployeeInheritsSalonHours() {
        Salon salon = salon();
        when(scheduleRepo.findByEmployeeId(4L)).thenReturn(Optional.empty());

        boolean available = availability.isEmployeeAvailable(
                4L,
                salon,
                next(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                60
        );

        assertTrue(available);
    }

    @Test
    void appointmentCannotCrossAConfiguredBreak() {
        Salon salon = salon();
        LocalDate monday = next(DayOfWeek.MONDAY);
        EmployeeSchedule schedule = schedule(
                period(DayOfWeek.MONDAY, 9, 0, 12, 0),
                period(DayOfWeek.MONDAY, 13, 0, 17, 0)
        );
        when(scheduleRepo.findByEmployeeId(4L)).thenReturn(Optional.of(schedule));

        assertFalse(availability.isEmployeeAvailable(
                4L,
                salon,
                monday,
                LocalTime.of(11, 30),
                60
        ));
        assertTrue(availability.isEmployeeAvailable(
                4L,
                salon,
                monday,
                LocalTime.of(13, 0),
                90
        ));
    }

    @Test
    void appointmentMustFinishBeforeSalonCloses() {
        assertFalse(availability.isSalonOpenFor(salon(), LocalTime.of(17, 30), 60));
        assertTrue(availability.isSalonOpenFor(salon(), LocalTime.of(17, 30), 30));
    }

    private EmployeeSchedule schedule(EmployeeWorkingPeriod... periods) {
        EmployeeSchedule schedule = new EmployeeSchedule();
        schedule.setId(1L);
        schedule.setPeriods(new ArrayList<>());
        for (EmployeeWorkingPeriod period : periods) {
            period.setSchedule(schedule);
            schedule.getPeriods().add(period);
        }
        return schedule;
    }

    private EmployeeWorkingPeriod period(
            DayOfWeek day,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
        return new EmployeeWorkingPeriod(
                null,
                null,
                day,
                LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute)
        );
    }

    private Salon salon() {
        Salon salon = new Salon();
        salon.setOpeningTime(LocalTime.of(9, 0));
        salon.setClosingTime(LocalTime.of(18, 0));
        return salon;
    }

    private LocalDate next(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }
}
