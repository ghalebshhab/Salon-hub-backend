package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.Services.Salon.SalonAccess;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.schedule.EmployeeTimeOff;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.EmployeeTimeOffRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeTimeOffManagementImpl implements EmployeeTimeOffManagement {

    static final int MAX_DAYS = 90;
    static final int MAX_DAYS_AHEAD = 365;
    private static final int MAX_REASON_LENGTH = 255;
    private static final Set<BookingStatus> AFFECTED_STATUSES =
            Set.of(BookingStatus.PENDING, BookingStatus.ACCEPTED);

    private final EmployeeTimeOffRepo timeOffRepo;
    private final BookingRepo bookingRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final Clock clock;
    private final SalonAccess salonAccess;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<EmployeeTimeOffResponse>> getUpcomingTimeOff(Long employeeId, Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        if (findSalonEmployee(employeeId, salon.getId()) == null) {
            return ApiResponse.error("Employee in your salon was not found");
        }
        List<EmployeeTimeOffResponse> items = timeOffRepo
                .findByEmployeeIdAndEndDateGreaterThanEqualOrderByStartDateAsc(employeeId, LocalDate.now(clock))
                .stream()
                .map(timeOff -> map(timeOff, 0))
                .toList();
        return ApiResponse.success("Employee time off fetched successfully", items);
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeTimeOffResponse> addTimeOff(
            Long employeeId,
            EmployeeTimeOffRequest request,
            Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        User employee = findSalonEmployee(employeeId, salon.getId());
        if (employee == null) {
            return ApiResponse.error("Employee in your salon was not found");
        }
        String error = validate(request);
        if (error != null) {
            return ApiResponse.error(error);
        }
        if (timeOffRepo.existsOverlapping(employeeId, request.startDate(), request.endDate())) {
            return ApiResponse.error("This time off overlaps another time off for the same employee");
        }

        EmployeeTimeOff timeOff = new EmployeeTimeOff();
        timeOff.setEmployee(employee);
        timeOff.setSalon(salon);
        timeOff.setStartDate(request.startDate());
        timeOff.setEndDate(request.endDate());
        timeOff.setReason(normalize(request.reason()));
        timeOff.setCreatedAt(LocalDateTime.now(clock));
        timeOffRepo.save(timeOff);

        int affected = (int) bookingRepo.countByAssignedEmployeeIdAndBookingDateBetweenAndStatusIn(
                employeeId,
                request.startDate(),
                request.endDate(),
                AFFECTED_STATUSES
        );
        String message = affected == 0
                ? "Time off saved"
                : "Time off saved. " + affected + " appointment(s) of this employee fall inside it and need another employee";
        return ApiResponse.success(message, map(timeOff, affected));
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeTimeOff(Long timeOffId, Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        EmployeeTimeOff timeOff = timeOffId == null ? null : timeOffRepo.findById(timeOffId).orElse(null);
        if (timeOff == null || !timeOff.getSalon().getId().equals(salon.getId())) {
            return ApiResponse.error("Time off in your salon was not found");
        }
        timeOffRepo.delete(timeOff);
        return ApiResponse.success("Time off removed", null);
    }

    private String validate(EmployeeTimeOffRequest request) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return "Start date and end date are required";
        }
        LocalDate today = LocalDate.now(clock);
        if (request.endDate().isBefore(request.startDate())) {
            return "End date cannot be before start date";
        }
        if (request.endDate().isBefore(today)) {
            return "Time off must include today or a future date";
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1 > MAX_DAYS) {
            return "Time off cannot be longer than " + MAX_DAYS + " days";
        }
        if (request.startDate().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            return "Time off can be planned at most one year ahead";
        }
        String reason = normalize(request.reason());
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            return "Reason cannot exceed " + MAX_REASON_LENGTH + " characters";
        }
        return null;
    }

    /** The owner's salon, or the salon of a manager (Business plan). */
    private Salon findManageableSalon(Long ownerId) {
        Salon salon = ownerId == null ? null : salonRepo.findByOwnerId(ownerId);
        if (salon == null) {
            return salonAccess.managerSalon(ownerId);
        }
        return salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner() ? salon : null;
    }

    private User findSalonEmployee(Long employeeId, Long salonId) {
        if (employeeId == null) {
            return null;
        }
        User employee = userRepo.findById(employeeId).orElse(null);
        return employee != null
                && employee.getRole() == UserRole.Employee
                && employee.getSalon() != null
                && employee.getSalon().getId().equals(salonId) ? employee : null;
    }

    private EmployeeTimeOffResponse map(EmployeeTimeOff timeOff, int affectedBookings) {
        return new EmployeeTimeOffResponse(
                timeOff.getId(),
                timeOff.getEmployee().getId(),
                timeOff.getStartDate(),
                timeOff.getEndDate(),
                timeOff.getReason(),
                affectedBookings
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
