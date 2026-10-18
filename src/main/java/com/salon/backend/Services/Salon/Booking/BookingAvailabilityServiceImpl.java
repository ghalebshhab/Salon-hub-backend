package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.AvailableBookingEmployeeResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilitySlotResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityServiceImpl implements BookingAvailabilityService {

    public static final int SLOT_INTERVAL_MINUTES = 15;
    public static final int MINIMUM_LEAD_MINUTES = 30;
    private static final int MAX_SERVICES_PER_BOOKING = 10;

    private final SalonRepo salonRepo;
    private final SalonServiceRepo salonServiceRepo;
    private final UserRepo userRepo;
    private final EmployeeServiceCapabilityRepo capabilityRepo;
    private final EmployeeScheduleRepo scheduleRepo;
    private final BookingRepo bookingRepo;
    private final EmployeeScheduleAvailability scheduleAvailability;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BookingAvailabilityResponse> getAvailability(
            BookingAvailabilityRequest request,
            Long requesterId) {
        if (findActiveUser(requesterId) == null) {
            return ApiResponse.error("Active user not found");
        }

        ApiResponse<Void> validation = validateRequest(request);
        if (!validation.isSuccess()) {
            return ApiResponse.error(validation.getMessage());
        }

        Salon salon = salonRepo.findById(request.salonId()).orElse(null);
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("Active salon not found");
        }

        Set<Long> serviceIds = new LinkedHashSet<>(request.serviceIds());
        List<SalonService> services = salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(
                List.copyOf(serviceIds),
                salon.getId()
        );
        if (services.size() != serviceIds.size()) {
            return ApiResponse.error("Every selected service must be active and belong to this salon");
        }

        int totalDuration = services.stream()
                .mapToInt(SalonService::getDurationMinutes)
                .sum();
        List<User> eligibleEmployees = findQualifiedEmployees(
                salon.getId(),
                serviceIds,
                request.employeeId()
        );
        if (request.employeeId() != null && eligibleEmployees.isEmpty()) {
            return ApiResponse.error("Selected employee cannot perform every requested service");
        }

        AvailabilityData data = loadAvailabilityData(eligibleEmployees, request.bookingDate(), null);
        List<BookingAvailabilitySlotResponse> slots = new ArrayList<>();
        LocalTime candidate = salon.getOpeningTime();
        while (candidate != null
                && scheduleAvailability.isSalonOpenFor(salon, candidate, totalDuration)) {
            List<User> availableEmployees = availableEmployeesAt(
                    eligibleEmployees,
                    data,
                    salon,
                    request.bookingDate(),
                    candidate,
                    totalDuration
            );
            if (meetsMinimumLeadTime(request.bookingDate(), candidate)
                    && !availableEmployees.isEmpty()) {
                slots.add(new BookingAvailabilitySlotResponse(
                        candidate,
                        availableEmployees.stream().map(this::mapEmployee).toList()
                ));
            }
            candidate = candidate.plusMinutes(SLOT_INTERVAL_MINUTES);
            if (candidate.equals(salon.getOpeningTime())) {
                break;
            }
        }

        return ApiResponse.success(
                "Available appointment times fetched successfully",
                new BookingAvailabilityResponse(
                        salon.getId(),
                        request.bookingDate(),
                        totalDuration,
                        SLOT_INTERVAL_MINUTES,
                        MINIMUM_LEAD_MINUTES,
                        slots
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BookingAvailabilityResponse> getRescheduleAvailability(
            Booking booking,
            LocalDate bookingDate) {
        if (booking == null || bookingDate == null) {
            return ApiResponse.error("Booking and reschedule date are required");
        }
        if (bookingDate.isBefore(LocalDate.now(clock))) {
            return ApiResponse.error("Reschedule date cannot be in the past");
        }

        Salon salon = booking.getSalon();
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("Active salon not found");
        }

        Set<Long> serviceIds = booking.getServiceItems().stream()
                .map(BookingServiceItem::getSourceServiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Long employeeId = booking.getAssignedEmployee() == null
                ? null
                : booking.getAssignedEmployee().getId();
        List<User> eligibleEmployees = findQualifiedEmployees(
                salon.getId(),
                serviceIds,
                employeeId
        );
        if (employeeId != null && eligibleEmployees.isEmpty()) {
            return ApiResponse.error("Assigned employee can no longer perform every requested service");
        }

        AvailabilityData data = loadAvailabilityData(
                eligibleEmployees,
                bookingDate,
                booking.getId()
        );
        List<BookingAvailabilitySlotResponse> slots = new ArrayList<>();
        LocalTime candidate = salon.getOpeningTime();
        while (candidate != null
                && scheduleAvailability.isSalonOpenFor(
                        salon,
                        candidate,
                        booking.getTotalDurationMinutes()
                )) {
            List<User> availableEmployees = availableEmployeesAt(
                    eligibleEmployees,
                    data,
                    salon,
                    bookingDate,
                    candidate,
                    booking.getTotalDurationMinutes()
            );
            if (meetsMinimumLeadTime(bookingDate, candidate)
                    && !availableEmployees.isEmpty()) {
                slots.add(new BookingAvailabilitySlotResponse(
                        candidate,
                        availableEmployees.stream().map(this::mapEmployee).toList()
                ));
            }
            candidate = candidate.plusMinutes(SLOT_INTERVAL_MINUTES);
            if (candidate.equals(salon.getOpeningTime())) {
                break;
            }
        }

        return ApiResponse.success(
                "Reschedule appointment times fetched successfully",
                new BookingAvailabilityResponse(
                        salon.getId(),
                        bookingDate,
                        booking.getTotalDurationMinutes(),
                        SLOT_INTERVAL_MINUTES,
                        MINIMUM_LEAD_MINUTES,
                        slots
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(
            Salon salon,
            Set<Long> serviceIds,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes,
            Long employeeId) {
        return isSlotAvailable(
                salon,
                serviceIds,
                bookingDate,
                bookingTime,
                durationMinutes,
                employeeId,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(
            Salon salon,
            Set<Long> serviceIds,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes,
            Long employeeId,
            Long excludedBookingId) {
        if (!isActiveSalon(salon)
                || serviceIds == null
                || serviceIds.isEmpty()
                || bookingDate == null
                || bookingTime == null
                || durationMinutes <= 0
                || !meetsMinimumLeadTime(bookingDate, bookingTime)
                || !isOnSlotGrid(salon, bookingTime)
                || !scheduleAvailability.isSalonOpenFor(salon, bookingTime, durationMinutes)) {
            return false;
        }

        List<User> eligibleEmployees = findQualifiedEmployees(
                salon.getId(),
                serviceIds,
                employeeId
        );
        if (eligibleEmployees.isEmpty()) {
            return false;
        }
        AvailabilityData data = loadAvailabilityData(
                eligibleEmployees,
                bookingDate,
                excludedBookingId
        );
        return !availableEmployeesAt(
                eligibleEmployees,
                data,
                salon,
                bookingDate,
                bookingTime,
                durationMinutes
        ).isEmpty();
    }

    private ApiResponse<Void> validateRequest(BookingAvailabilityRequest request) {
        if (request == null) {
            return ApiResponse.error("Availability information is required");
        }
        if (request.salonId() == null) {
            return ApiResponse.error("Salon id is required");
        }
        if (request.serviceIds() == null || request.serviceIds().isEmpty()) {
            return ApiResponse.error("At least one service is required");
        }
        if (request.serviceIds().size() > MAX_SERVICES_PER_BOOKING) {
            return ApiResponse.error("Availability cannot contain more than 10 services");
        }
        if (request.serviceIds().stream().anyMatch(id -> id == null)) {
            return ApiResponse.error("Every service id is required");
        }
        if (new HashSet<>(request.serviceIds()).size() != request.serviceIds().size()) {
            return ApiResponse.error("A service can be selected only once");
        }
        if (request.bookingDate() == null) {
            return ApiResponse.error("Booking date is required");
        }
        if (request.bookingDate().isBefore(LocalDate.now(clock))) {
            return ApiResponse.error("Booking date cannot be in the past");
        }
        return ApiResponse.success("Availability information is valid", null);
    }

    private List<User> findQualifiedEmployees(
            Long salonId,
            Set<Long> serviceIds,
            Long requestedEmployeeId) {
        List<User> employees = userRepo
                .findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
                        salonId,
                        UserRole.Employee,
                        UserStatus.Active
                );
        if (requestedEmployeeId != null) {
            employees = employees.stream()
                    .filter(employee -> requestedEmployeeId.equals(employee.getId()))
                    .toList();
        }
        if (employees.isEmpty()) {
            return List.of();
        }

        Set<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Set<Long>> capabilityIdsByEmployee = new HashMap<>();
        for (EmployeeServiceCapability capability : capabilityRepo.findByEmployeeIdIn(employeeIds)) {
            capabilityIdsByEmployee
                    .computeIfAbsent(capability.getEmployee().getId(), ignored -> new HashSet<>())
                    .add(capability.getService().getId());
        }
        return employees.stream()
                .filter(employee -> capabilityIdsByEmployee
                        .getOrDefault(employee.getId(), Set.of())
                        .containsAll(serviceIds))
                .toList();
    }

    private AvailabilityData loadAvailabilityData(
            List<User> employees,
            LocalDate bookingDate,
            Long excludedBookingId) {
        Set<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toSet());
        if (employeeIds.isEmpty()) {
            return new AvailabilityData(Map.of(), Map.of());
        }

        Map<Long, EmployeeSchedule> schedules = scheduleRepo.findByEmployeeIdIn(employeeIds)
                .stream()
                .collect(Collectors.toMap(
                        schedule -> schedule.getEmployee().getId(),
                        Function.identity()
                ));
        Map<Long, List<Booking>> bookingsByEmployee = bookingRepo
                .findByAssignedEmployeeIdInAndBookingDateAndStatus(
                        employeeIds,
                        bookingDate,
                        BookingStatus.ACCEPTED
                ).stream()
                .filter(booking -> excludedBookingId == null
                        || !excludedBookingId.equals(booking.getId()))
                .collect(Collectors.groupingBy(booking -> booking.getAssignedEmployee().getId()));
        return new AvailabilityData(schedules, bookingsByEmployee);
    }

    private List<User> availableEmployeesAt(
            Collection<User> employees,
            AvailabilityData data,
            Salon salon,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes) {
        return employees.stream()
                .filter(employee -> scheduleAvailability.isEmployeeAvailable(
                        data.schedules().get(employee.getId()),
                        salon,
                        bookingDate,
                        bookingTime,
                        durationMinutes
                ))
                .filter(employee -> !hasConflict(
                        data.bookingsByEmployee().getOrDefault(employee.getId(), List.of()),
                        bookingDate,
                        bookingTime,
                        durationMinutes
                ))
                .toList();
    }

    private boolean hasConflict(
            List<Booking> acceptedBookings,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes) {
        LocalDateTime candidateStart = LocalDateTime.of(bookingDate, bookingTime);
        LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
        return acceptedBookings.stream().anyMatch(existing -> {
            LocalDateTime existingStart = LocalDateTime.of(
                    existing.getBookingDate(),
                    existing.getBookingTime()
            );
            LocalDateTime existingEnd = existingStart.plusMinutes(existing.getTotalDurationMinutes());
            return existingStart.isBefore(candidateEnd) && candidateStart.isBefore(existingEnd);
        });
    }

    private boolean meetsMinimumLeadTime(LocalDate date, LocalTime time) {
        LocalDateTime appointment = LocalDateTime.of(date, time);
        return !appointment.isBefore(LocalDateTime.now(clock).plusMinutes(MINIMUM_LEAD_MINUTES));
    }

    private boolean isOnSlotGrid(Salon salon, LocalTime time) {
        long minutesFromOpening = Duration.between(salon.getOpeningTime(), time).toMinutes();
        return minutesFromOpening >= 0 && minutesFromOpening % SLOT_INTERVAL_MINUTES == 0;
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepo.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.Active)
                .orElse(null);
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner()
                && salon.getOpeningTime() != null
                && salon.getClosingTime() != null;
    }

    private AvailableBookingEmployeeResponse mapEmployee(User employee) {
        return new AvailableBookingEmployeeResponse(
                employee.getId(),
                fullName(employee),
                employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                        ? null
                        : employee.getCareerSkills().get(0)
        );
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? user.getUserName() : name;
    }

    private record AvailabilityData(
            Map<Long, EmployeeSchedule> schedules,
            Map<Long, List<Booking>> bookingsByEmployee
    ) {
    }
}
