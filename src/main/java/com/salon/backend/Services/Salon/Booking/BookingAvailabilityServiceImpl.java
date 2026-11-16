package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.AssignableEmployeeResponse;
import com.salon.backend.DTOs.Salon.Booking.AvailableBookingEmployeeResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilitySlotResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingUnavailableReason;
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
import com.salon.backend.Repositories.Salon.EmployeeTimeOffRepo;
import com.salon.backend.Repositories.Salon.SalonClosureRepo;
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
import java.util.Comparator;
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
    /** How far ahead the "next free day" search looks. */
    public static final int NEXT_AVAILABLE_SEARCH_DAYS = 14;
    private static final int MAX_SERVICES_PER_BOOKING = 10;

    /**
     * Statuses that occupy an employee's time. A pending request that names an
     * employee holds that time, so two customers cannot request the same slot.
     */
    public static final Set<BookingStatus> BUSY_STATUSES = Set.of(
            BookingStatus.PENDING,
            BookingStatus.ACCEPTED,
            BookingStatus.IN_PROGRESS
    );

    private final SalonRepo salonRepo;
    private final SalonServiceRepo salonServiceRepo;
    private final UserRepo userRepo;
    private final EmployeeServiceCapabilityRepo capabilityRepo;
    private final EmployeeScheduleRepo scheduleRepo;
    private final BookingRepo bookingRepo;
    private final EmployeeTimeOffRepo timeOffRepo;
    private final SalonClosureRepo closureRepo;
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

        return ApiResponse.success(
                "Available appointment times fetched successfully",
                buildResponse(salon, eligibleEmployees, request.bookingDate(), totalDuration,
                        null, request.employeeId())
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

        Long employeeId = booking.getAssignedEmployee() == null
                ? null
                : booking.getAssignedEmployee().getId();
        List<User> eligibleEmployees = findQualifiedEmployees(
                salon.getId(),
                serviceIdsOf(booking),
                employeeId
        );
        if (employeeId != null && eligibleEmployees.isEmpty()) {
            return ApiResponse.error("Assigned employee can no longer perform every requested service");
        }

        return ApiResponse.success(
                "Reschedule appointment times fetched successfully",
                buildResponse(salon, eligibleEmployees, bookingDate,
                        booking.getTotalDurationMinutes(), booking.getId(), employeeId)
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
                salon,
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

    @Override
    @Transactional(readOnly = true)
    public List<AssignableEmployeeResponse> getAssignableEmployees(Booking booking) {
        if (booking == null || booking.getSalon() == null) {
            return List.of();
        }
        List<User> qualified = findQualifiedEmployees(
                booking.getSalon().getId(),
                serviceIdsOf(booking),
                null
        );
        AvailabilityData data = loadAvailabilityData(
                booking.getSalon(), qualified, booking.getBookingDate(), booking.getId());
        return qualified.stream()
                .map(employee -> {
                    String reason = unavailableReason(employee, data, booking);
                    return new AssignableEmployeeResponse(
                            employee.getId(),
                            fullName(employee),
                            specialty(employee),
                            reason == null,
                            reason
                    );
                })
                .sorted(Comparator.comparing(AssignableEmployeeResponse::available).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String employeeUnavailableReason(Long employeeId, Booking booking) {
        if (employeeId == null || booking == null) {
            return BookingUnavailableReason.NO_QUALIFIED_EMPLOYEE;
        }
        User employee = userRepo.findById(employeeId).orElse(null);
        if (employee == null) {
            return BookingUnavailableReason.NO_QUALIFIED_EMPLOYEE;
        }
        AvailabilityData data = loadAvailabilityData(
                booking.getSalon(), List.of(employee), booking.getBookingDate(), booking.getId());
        return unavailableReason(employee, data, booking);
    }

    private BookingAvailabilityResponse buildResponse(
            Salon salon,
            List<User> eligibleEmployees,
            LocalDate date,
            int duration,
            Long excludedBookingId,
            Long requestedEmployeeId) {
        List<BookingAvailabilitySlotResponse> slots =
                computeSlots(salon, eligibleEmployees, date, duration, excludedBookingId);
        String reason = null;
        LocalDate nextAvailable = null;
        if (slots.isEmpty()) {
            reason = emptyDayReason(salon, eligibleEmployees, date, duration, requestedEmployeeId);
            nextAvailable = findNextAvailableDate(salon, eligibleEmployees, date, duration, excludedBookingId);
        }
        return new BookingAvailabilityResponse(
                salon.getId(),
                date,
                duration,
                SLOT_INTERVAL_MINUTES,
                MINIMUM_LEAD_MINUTES,
                slots,
                reason,
                nextAvailable
        );
    }

    /** Every start time on the salon's 15-minute grid with at least one free employee. */
    private List<BookingAvailabilitySlotResponse> computeSlots(
            Salon salon,
            List<User> eligibleEmployees,
            LocalDate date,
            int duration,
            Long excludedBookingId) {
        if (eligibleEmployees.isEmpty() || closureRepo.isSalonClosedOn(salon.getId(), date)) {
            return List.of();
        }
        AvailabilityData data = loadAvailabilityData(salon, eligibleEmployees, date, excludedBookingId);
        List<BookingAvailabilitySlotResponse> slots = new ArrayList<>();
        LocalTime candidate = salon.getOpeningTime();
        while (candidate != null && scheduleAvailability.isSalonOpenFor(salon, candidate, duration)) {
            if (meetsMinimumLeadTime(date, candidate)) {
                List<User> available = availableEmployeesAt(
                        eligibleEmployees, data, salon, date, candidate, duration);
                if (!available.isEmpty()) {
                    slots.add(new BookingAvailabilitySlotResponse(
                            candidate,
                            available.stream().map(this::mapEmployee).toList()
                    ));
                }
            }
            candidate = candidate.plusMinutes(SLOT_INTERVAL_MINUTES);
            if (candidate.equals(salon.getOpeningTime())) {
                break;
            }
        }
        return slots;
    }

    private LocalDate findNextAvailableDate(
            Salon salon,
            List<User> eligibleEmployees,
            LocalDate fromDate,
            int duration,
            Long excludedBookingId) {
        if (eligibleEmployees.isEmpty()) {
            return null;
        }
        for (int offset = 1; offset <= NEXT_AVAILABLE_SEARCH_DAYS; offset++) {
            LocalDate date = fromDate.plusDays(offset);
            if (!computeSlots(salon, eligibleEmployees, date, duration, excludedBookingId).isEmpty()) {
                return date;
            }
        }
        return null;
    }

    private String emptyDayReason(
            Salon salon,
            List<User> eligibleEmployees,
            LocalDate date,
            int duration,
            Long requestedEmployeeId) {
        if (closureRepo.isSalonClosedOn(salon.getId(), date)) {
            return BookingUnavailableReason.SALON_CLOSED;
        }
        if (eligibleEmployees.isEmpty()) {
            return BookingUnavailableReason.NO_QUALIFIED_EMPLOYEE;
        }
        if (requestedEmployeeId != null && eligibleEmployees.size() == 1) {
            User employee = eligibleEmployees.get(0);
            if (!timeOffRepo.findEmployeeIdsOffOn(List.of(employee.getId()), date).isEmpty()) {
                return BookingUnavailableReason.TIME_OFF;
            }
            EmployeeSchedule schedule = scheduleRepo.findByEmployeeIdIn(Set.of(employee.getId()))
                    .stream().findFirst().orElse(null);
            if (schedule != null && schedule.getPeriods().stream()
                    .noneMatch(period -> period.getDayOfWeek() == date.getDayOfWeek())) {
                return BookingUnavailableReason.NOT_WORKING;
            }
        }
        LocalTime lastStart = latestStart(salon, duration);
        if (date.equals(LocalDate.now(clock))
                && (lastStart == null || !meetsMinimumLeadTime(date, lastStart))) {
            return BookingUnavailableReason.NO_TIME_LEFT_TODAY;
        }
        return BookingUnavailableReason.FULLY_BOOKED;
    }

    private LocalTime latestStart(Salon salon, int duration) {
        LocalTime candidate = salon.getOpeningTime();
        LocalTime last = null;
        while (candidate != null && scheduleAvailability.isSalonOpenFor(salon, candidate, duration)) {
            last = candidate;
            candidate = candidate.plusMinutes(SLOT_INTERVAL_MINUTES);
            if (candidate.equals(salon.getOpeningTime())) {
                break;
            }
        }
        return last;
    }

    private String unavailableReason(User employee, AvailabilityData data, Booking booking) {
        if (data.salonClosed()) {
            return BookingUnavailableReason.SALON_CLOSED;
        }
        if (data.employeesOff().contains(employee.getId())) {
            return BookingUnavailableReason.TIME_OFF;
        }
        if (!scheduleAvailability.isEmployeeAvailable(
                data.schedules().get(employee.getId()),
                booking.getSalon(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTotalDurationMinutes())) {
            return BookingUnavailableReason.NOT_WORKING;
        }
        if (hasConflict(
                data.bookingsByEmployee().getOrDefault(employee.getId(), List.of()),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTotalDurationMinutes())) {
            return BookingUnavailableReason.BUSY;
        }
        return null;
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

    private Set<Long> serviceIdsOf(Booking booking) {
        return booking.getServiceItems().stream()
                .map(BookingServiceItem::getSourceServiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
            Salon salon,
            List<User> employees,
            LocalDate bookingDate,
            Long excludedBookingId) {
        boolean salonClosed = salon != null && closureRepo.isSalonClosedOn(salon.getId(), bookingDate);
        Set<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toSet());
        if (employeeIds.isEmpty() || salonClosed) {
            return new AvailabilityData(Map.of(), Map.of(), Set.of(), salonClosed);
        }

        Map<Long, EmployeeSchedule> schedules = scheduleRepo.findByEmployeeIdIn(employeeIds)
                .stream()
                .collect(Collectors.toMap(
                        schedule -> schedule.getEmployee().getId(),
                        Function.identity()
                ));
        Map<Long, List<Booking>> bookingsByEmployee = bookingRepo
                .findByAssignedEmployeeIdInAndBookingDateAndStatusIn(
                        employeeIds,
                        bookingDate,
                        BUSY_STATUSES
                ).stream()
                .filter(booking -> excludedBookingId == null
                        || !excludedBookingId.equals(booking.getId()))
                .collect(Collectors.groupingBy(booking -> booking.getAssignedEmployee().getId()));
        Set<Long> employeesOff = new HashSet<>(timeOffRepo.findEmployeeIdsOffOn(employeeIds, bookingDate));
        return new AvailabilityData(schedules, bookingsByEmployee, employeesOff, false);
    }

    private List<User> availableEmployeesAt(
            Collection<User> employees,
            AvailabilityData data,
            Salon salon,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes) {
        if (data.salonClosed()) {
            return List.of();
        }
        return employees.stream()
                .filter(employee -> !data.employeesOff().contains(employee.getId()))
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
            List<Booking> busyBookings,
            LocalDate bookingDate,
            LocalTime bookingTime,
            int durationMinutes) {
        LocalDateTime candidateStart = LocalDateTime.of(bookingDate, bookingTime);
        LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
        return busyBookings.stream().anyMatch(existing -> {
            LocalDateTime existingStart = LocalDateTime.of(
                    existing.getBookingDate(),
                    existing.getBookingTime()
            );
            LocalDateTime existingEnd = effectiveEnd(existing, existingStart);
            return existingStart.isBefore(candidateEnd) && candidateStart.isBefore(existingEnd);
        });
    }

    /** A running service that was extended occupies the employee until its expected end. */
    private LocalDateTime effectiveEnd(Booking booking, LocalDateTime start) {
        LocalDateTime plannedEnd = start.plusMinutes(booking.getTotalDurationMinutes());
        LocalDateTime expectedEnd = booking.getStatus() == BookingStatus.IN_PROGRESS
                ? booking.getTracking().getExpectedEndAt()
                : null;
        return expectedEnd != null && expectedEnd.isAfter(plannedEnd) ? expectedEnd : plannedEnd;
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
                specialty(employee)
        );
    }

    private String specialty(User employee) {
        return employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                ? null
                : employee.getCareerSkills().get(0);
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? user.getUserName() : name;
    }

    private record AvailabilityData(
            Map<Long, EmployeeSchedule> schedules,
            Map<Long, List<Booking>> bookingsByEmployee,
            Set<Long> employeesOff,
            boolean salonClosed
    ) {
    }
}
