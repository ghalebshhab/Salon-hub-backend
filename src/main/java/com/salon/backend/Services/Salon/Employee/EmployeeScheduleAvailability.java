package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class EmployeeScheduleAvailability {

    private final EmployeeScheduleRepo scheduleRepo;

    public boolean isSalonOpenFor(
            Salon salon,
            LocalTime startTime,
            int durationMinutes) {
        if (salon == null
                || salon.getOpeningTime() == null
                || salon.getClosingTime() == null
                || startTime == null
                || durationMinutes <= 0) {
            return false;
        }

        LocalDate referenceDate = LocalDate.of(2000, 1, 1);
        LocalDateTime start = LocalDateTime.of(referenceDate, startTime);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        return !startTime.isBefore(salon.getOpeningTime())
                && end.toLocalDate().equals(referenceDate)
                && !end.toLocalTime().isAfter(salon.getClosingTime());
    }

    public boolean isEmployeeAvailable(
            Long employeeId,
            Salon salon,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes) {
        if (employeeId == null
                || date == null
                || !isSalonOpenFor(salon, startTime, durationMinutes)) {
            return false;
        }

        EmployeeSchedule schedule = scheduleRepo.findByEmployeeId(employeeId).orElse(null);
        return isEmployeeAvailable(schedule, salon, date, startTime, durationMinutes);
    }

    public boolean isEmployeeAvailable(
            EmployeeSchedule schedule,
            Salon salon,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes) {
        if (date == null || !isSalonOpenFor(salon, startTime, durationMinutes)) {
            return false;
        }
        if (schedule == null) {
            return true;
        }

        LocalDateTime appointmentStart = LocalDateTime.of(date, startTime);
        LocalDateTime appointmentEnd = appointmentStart.plusMinutes(durationMinutes);

        return schedule.getPeriods().stream()
                .filter(period -> period.getDayOfWeek() == date.getDayOfWeek())
                .anyMatch(period -> {
                    LocalDateTime periodStart = LocalDateTime.of(date, period.getStartTime());
                    LocalDateTime periodEnd = LocalDateTime.of(date, period.getEndTime());
                    return !appointmentStart.isBefore(periodStart)
                            && !appointmentEnd.isAfter(periodEnd);
                });
    }
}
