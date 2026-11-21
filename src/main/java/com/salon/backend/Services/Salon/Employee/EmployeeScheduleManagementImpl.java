package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.Services.Salon.SalonAccess;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeScheduleResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeWorkingPeriodRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeWorkingPeriodResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeScheduleRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
import com.salon.backend.Entities.salons.schedule.EmployeeWorkingPeriod;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeScheduleManagementImpl implements EmployeeScheduleManagement {

    private static final int MAX_WEEKLY_PERIODS = 28;

    private final EmployeeScheduleRepo scheduleRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final SalonAccess salonAccess;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmployeeScheduleResponse> getEmployeeSchedule(Long employeeId, Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        User employee = findSalonEmployee(employeeId, salon.getId(), false);
        if (employee == null) {
            return ApiResponse.error("Employee in your salon was not found");
        }

        EmployeeSchedule schedule = scheduleRepo.findByEmployeeId(employeeId).orElse(null);
        return ApiResponse.success(
                "Employee schedule fetched successfully",
                mapResponse(employee, salon, schedule)
        );
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeScheduleResponse> updateEmployeeSchedule(
            Long employeeId,
            UpdateEmployeeScheduleRequest request,
            Long ownerId) {

        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        User employee = findSalonEmployee(employeeId, salon.getId(), true);
        if (employee == null) {
            return ApiResponse.error("Active employee in your salon was not found");
        }
        if (request == null || request.periods() == null) {
            return ApiResponse.error("Working periods are required; use an empty list for no working hours");
        }
        if (request.periods().size() > MAX_WEEKLY_PERIODS) {
            return ApiResponse.error("An employee schedule cannot contain more than 28 weekly periods");
        }

        ApiResponse<Void> validation = validatePeriods(request.periods(), salon);
        if (!validation.isSuccess()) {
            return ApiResponse.error(validation.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        EmployeeSchedule schedule = scheduleRepo.findByEmployeeIdForUpdate(employeeId)
                .orElseGet(() -> {
                    EmployeeSchedule newSchedule = new EmployeeSchedule();
                    newSchedule.setEmployee(employee);
                    newSchedule.setCreatedAt(now);
                    newSchedule.setPeriods(new ArrayList<>());
                    return newSchedule;
                });

        List<EmployeeWorkingPeriod> periods = request.periods().stream()
                .sorted(periodComparator())
                .map(item -> new EmployeeWorkingPeriod(
                        null,
                        schedule,
                        item.dayOfWeek(),
                        item.startTime(),
                        item.endTime()
                ))
                .toList();

        schedule.replacePeriods(periods);
        schedule.setUpdatedAt(now);
        scheduleRepo.save(schedule);

        return ApiResponse.success(
                "Employee schedule updated successfully",
                mapResponse(employee, salon, schedule)
        );
    }

    private ApiResponse<Void> validatePeriods(
            List<EmployeeWorkingPeriodRequest> periods,
            Salon salon) {

        Map<DayOfWeek, List<EmployeeWorkingPeriodRequest>> periodsByDay =
                new EnumMap<>(DayOfWeek.class);

        for (EmployeeWorkingPeriodRequest period : periods) {
            if (period == null
                    || period.dayOfWeek() == null
                    || period.startTime() == null
                    || period.endTime() == null) {
                return ApiResponse.error("Day, start time, and end time are required for every period");
            }
            if (!period.startTime().isBefore(period.endTime())) {
                return ApiResponse.error("Working period start time must be before end time");
            }
            if (period.startTime().isBefore(salon.getOpeningTime())
                    || period.endTime().isAfter(salon.getClosingTime())) {
                return ApiResponse.error("Every working period must be inside the salon opening hours");
            }
            periodsByDay.computeIfAbsent(period.dayOfWeek(), ignored -> new ArrayList<>())
                    .add(period);
        }

        for (List<EmployeeWorkingPeriodRequest> dailyPeriods : periodsByDay.values()) {
            dailyPeriods.sort(Comparator.comparing(EmployeeWorkingPeriodRequest::startTime));
            for (int index = 1; index < dailyPeriods.size(); index++) {
                EmployeeWorkingPeriodRequest previous = dailyPeriods.get(index - 1);
                EmployeeWorkingPeriodRequest current = dailyPeriods.get(index);
                if (current.startTime().isBefore(previous.endTime())) {
                    return ApiResponse.error("Working periods on the same day cannot overlap");
                }
            }
        }

        return ApiResponse.success("Employee schedule is valid", null);
    }

    /** The owner's salon, or the salon of a manager (Business plan). */
    private Salon findManageableSalon(Long ownerId) {
        Salon salon = ownerId == null ? null : salonRepo.findByOwnerId(ownerId);
        if (salon != null) {
            return isActiveSalon(salon) ? salon : null;
        }
        return salonAccess.managerSalon(ownerId);
    }

    private User findSalonEmployee(Long employeeId, Long salonId, boolean activeRequired) {
        if (employeeId == null) {
            return null;
        }
        User employee = activeRequired
                ? userRepo.findByIdForUpdate(employeeId).orElse(null)
                : userRepo.findById(employeeId).orElse(null);
        if (employee == null
                || employee.getRole() != UserRole.Employee
                || employee.getSalon() == null
                || !employee.getSalon().getId().equals(salonId)
                || (activeRequired && employee.getStatus() != UserStatus.Active)) {
            return null;
        }
        return employee;
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private EmployeeScheduleResponse mapResponse(
            User employee,
            Salon salon,
            EmployeeSchedule schedule) {

        List<EmployeeWorkingPeriodResponse> periods = schedule == null
                ? List.of()
                : schedule.getPeriods().stream()
                .sorted(Comparator.comparing(EmployeeWorkingPeriod::getDayOfWeek)
                        .thenComparing(EmployeeWorkingPeriod::getStartTime))
                .map(period -> new EmployeeWorkingPeriodResponse(
                        period.getId(),
                        period.getDayOfWeek(),
                        period.getStartTime(),
                        period.getEndTime()
                ))
                .toList();

        return new EmployeeScheduleResponse(
                employee.getId(),
                fullName(employee),
                salon.getId(),
                schedule != null,
                salon.getOpeningTime(),
                salon.getClosingTime(),
                periods
        );
    }

    private Comparator<EmployeeWorkingPeriodRequest> periodComparator() {
        return Comparator.comparing(EmployeeWorkingPeriodRequest::dayOfWeek)
                .thenComparing(EmployeeWorkingPeriodRequest::startTime);
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserName() : fullName;
    }
}
