package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingActionRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingServiceItemResponse;
import com.salon.backend.DTOs.Salon.Booking.CreateBookingRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.Employee.EmployeeScheduleAvailability;
import com.salon.backend.Services.Notification.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingManagementImpl implements BookingManagement {

    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int MAX_LOCATION_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_SERVICES_PER_BOOKING = 10;

    private final BookingRepo bookingRepo;
    private final SalonRepo salonRepo;
    private final SalonServiceRepo salonServiceRepo;
    private final UserRepo userRepo;
    private final EmployeeServiceCapabilityRepo capabilityRepo;
    private final EmployeeScheduleAvailability scheduleAvailability;
    private final BookingNotificationService bookingNotificationService;

    @Override
    @Transactional
    public ApiResponse<BookingResponse> createBooking(
            CreateBookingRequest request,
            Long customerId) {

        if (request == null) {
            return ApiResponse.error("Booking information is required");
        }

        User customer = findActiveUser(customerId);
        if (customer == null) {
            return ApiResponse.error("Active customer not found");
        }

        Salon salon = request.salonId() == null
                ? null
                : salonRepo.findById(request.salonId()).orElse(null);
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("Active salon not found");
        }

        ApiResponse<Void> requestValidation = validateCreateRequest(request);
        if (!requestValidation.isSuccess()) {
            return ApiResponse.error(requestValidation.getMessage());
        }

        Set<Long> uniqueServiceIds = new LinkedHashSet<>(request.serviceIds());
        if (uniqueServiceIds.size() != request.serviceIds().size()) {
            return ApiResponse.error("A service can be selected only once per booking");
        }

        List<SalonService> services = salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(
                List.copyOf(uniqueServiceIds),
                salon.getId()
        );
        if (services.size() != uniqueServiceIds.size()) {
            return ApiResponse.error("Every selected service must be active and belong to this salon");
        }

        User assignedEmployee = null;
        if (request.assignedEmployeeId() != null) {
            assignedEmployee = findAvailableSalonEmployee(request.assignedEmployeeId(), salon.getId());
            if (assignedEmployee == null) {
                return ApiResponse.error("Selected employee is not active in this salon");
            }
            if (!canPerformAllServices(assignedEmployee.getId(), uniqueServiceIds)) {
                return ApiResponse.error("Selected employee cannot perform every requested service");
            }
        }

        String customerPhone = normalizeText(request.customerPhoneNumber());
        if (customerPhone == null) {
            customerPhone = normalizeText(customer.getPhoneNumber());
        }
        if (customerPhone == null) {
            return ApiResponse.error("Customer phone number is required");
        }
        if (customerPhone.length() > MAX_PHONE_LENGTH) {
            return ApiResponse.error("Customer phone number cannot exceed 30 characters");
        }

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSalon(salon);
        booking.setAssignedEmployee(assignedEmployee);
        booking.setCustomerPhoneNumber(customerPhone);
        booking.setCustomerLocation(request.customerLocation().trim());
        booking.setBookingDate(request.bookingDate());
        booking.setBookingTime(request.bookingTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setNote(normalizeText(request.note()));
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setTotalDurationMinutes(0);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalDuration = 0;
        for (SalonService service : services) {
            BookingServiceItem item = new BookingServiceItem();
            item.setSourceServiceId(service.getId());
            item.setServiceName(service.getName());
            item.setPrice(service.getPrice());
            item.setDurationMinutes(service.getDurationMinutes());
            booking.addServiceItem(item);

            totalPrice = totalPrice.add(service.getPrice());
            totalDuration += service.getDurationMinutes();
        }

        booking.setTotalPrice(totalPrice);
        booking.setTotalDurationMinutes(totalDuration);

        if (!scheduleAvailability.isSalonOpenFor(
                salon,
                request.bookingTime(),
                totalDuration
        )) {
            return ApiResponse.error("The requested appointment must fit inside the salon opening hours");
        }
        if (assignedEmployee != null && !scheduleAvailability.isEmployeeAvailable(
                assignedEmployee.getId(),
                salon,
                request.bookingDate(),
                request.bookingTime(),
                totalDuration
        )) {
            return ApiResponse.error("Selected employee is not working for the full requested appointment");
        }

        bookingRepo.save(booking);

        return ApiResponse.success("Booking request created successfully", mapToResponse(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getMyBookings(Long customerId) {
        if (findActiveUser(customerId) == null) {
            return ApiResponse.error("Active customer not found");
        }
        return ApiResponse.success(
                "Customer bookings fetched successfully",
                mapBookings(bookingRepo.findByCustomerIdOrderByCreatedAtDesc(customerId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getMySalonBookings(Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        return ApiResponse.success(
                "Salon bookings fetched successfully",
                mapBookings(bookingRepo.findBySalonOwnerIdOrderByBookingDateDescBookingTimeDesc(ownerId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getMySalonPendingBookings(Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        return ApiResponse.success(
                "Pending salon bookings fetched successfully",
                mapBookings(bookingRepo.findBySalonOwnerIdAndStatusOrderByBookingDateAscBookingTimeAsc(
                        ownerId,
                        BookingStatus.PENDING
                ))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getMyEmployeeBookings(Long employeeId) {
        User employee = findActiveUser(employeeId);
        if (employee == null || employee.getRole() != UserRole.Employee || employee.getSalon() == null) {
            return ApiResponse.error("You are not an active salon employee");
        }
        return ApiResponse.success(
                "Employee bookings fetched successfully",
                mapBookings(bookingRepo.findByAssignedEmployeeIdOrderByBookingDateDescBookingTimeDesc(employeeId))
        );
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> acceptBooking(
            Long bookingId,
            BookingActionRequest request,
            Long ownerId) {

        Booking booking = findOwnedBookingForUpdate(bookingId, ownerId);
        if (booking == null) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return ApiResponse.error("Only a pending booking can be accepted");
        }
        if (!LocalDateTime.of(booking.getBookingDate(), booking.getBookingTime()).isAfter(LocalDateTime.now())) {
            return ApiResponse.error("A booking with a past appointment time cannot be accepted");
        }

        ApiResponse<Void> actionValidation = validateOwnerAction(request, false);
        if (!actionValidation.isSuccess()) {
            return ApiResponse.error(actionValidation.getMessage());
        }

        User assignedEmployee = booking.getAssignedEmployee();
        if (request != null && request.assignedEmployeeId() != null) {
            assignedEmployee = findAvailableSalonEmployeeForUpdate(
                    request.assignedEmployeeId(),
                    booking.getSalon().getId()
            );
            if (assignedEmployee == null) {
                return ApiResponse.error("Selected employee is not active in this salon");
            }
        } else if (assignedEmployee != null) {
            assignedEmployee = findAvailableSalonEmployeeForUpdate(
                    assignedEmployee.getId(),
                    booking.getSalon().getId()
            );
            if (assignedEmployee == null) {
                return ApiResponse.error("Selected employee is not active in this salon");
            }
        }

        if (assignedEmployee != null) {
            if (!canPerformAllBookingServices(assignedEmployee.getId(), booking)) {
                return ApiResponse.error("Selected employee cannot perform every requested service");
            }
            if (!scheduleAvailability.isEmployeeAvailable(
                    assignedEmployee.getId(),
                    booking.getSalon(),
                    booking.getBookingDate(),
                    booking.getBookingTime(),
                    booking.getTotalDurationMinutes()
            )) {
                return ApiResponse.error("Selected employee is not working for the full requested appointment");
            }
            if (hasAcceptedBookingConflict(assignedEmployee.getId(), booking)) {
                return ApiResponse.error("Selected employee already has an accepted appointment during this time");
            }
        } else if (!scheduleAvailability.isSalonOpenFor(
                booking.getSalon(),
                booking.getBookingTime(),
                booking.getTotalDurationMinutes()
        )) {
            return ApiResponse.error("The requested appointment must fit inside the salon opening hours");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setAssignedEmployee(assignedEmployee);
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setOwnerNote(request == null ? null : normalizeText(request.ownerNote()));
        booking.setOwnerSuggestedTime(request == null ? null : request.ownerSuggestedTime());
        booking.setDecidedAt(now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        bookingNotificationService.bookingAccepted(booking);

        return ApiResponse.success("Booking accepted successfully", mapToResponse(booking));
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> rejectBooking(
            Long bookingId,
            BookingActionRequest request,
            Long ownerId) {

        Booking booking = findOwnedBookingForUpdate(bookingId, ownerId);
        if (booking == null) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return ApiResponse.error("Only a pending booking can be rejected");
        }

        ApiResponse<Void> actionValidation = validateOwnerAction(request, true);
        if (!actionValidation.isSuccess()) {
            return ApiResponse.error(actionValidation.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setOwnerNote(request.ownerNote().trim());
        booking.setOwnerSuggestedTime(request.ownerSuggestedTime());
        booking.setDecidedAt(now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        bookingNotificationService.bookingRejected(booking);

        return ApiResponse.success("Booking rejected successfully", mapToResponse(booking));
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> cancelBooking(Long bookingId, Long customerId) {
        Booking booking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null || customerId == null || booking.getCustomer().getId() != customerId) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            return ApiResponse.error("Only a pending or accepted booking can be cancelled");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        bookingNotificationService.bookingCancelled(booking);

        return ApiResponse.success("Booking cancelled successfully", mapToResponse(booking));
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> completeBooking(Long bookingId, Long ownerId) {
        Booking booking = findOwnedBookingForUpdate(bookingId, ownerId);
        if (booking == null) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            return ApiResponse.error("Only an accepted booking can be completed");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);

        return ApiResponse.success("Booking completed successfully", mapToResponse(booking));
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> assignEmployee(
            Long bookingId,
            Long employeeId,
            Long ownerId) {
        return assignEmployee(bookingId, employeeId, null, ownerId);
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponse> assignEmployee(
            Long bookingId,
            Long employeeId,
            String ownerNote,
            Long ownerId) {

        Booking booking = findOwnedBookingForUpdate(bookingId, ownerId);
        if (booking == null) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            return ApiResponse.error("An employee can be assigned only to a pending or accepted booking");
        }

        User employee = findAvailableSalonEmployeeForUpdate(employeeId, booking.getSalon().getId());
        if (employee == null) {
            return ApiResponse.error("Selected employee is not active in this salon");
        }
        if (!canPerformAllBookingServices(employee.getId(), booking)) {
            return ApiResponse.error("Selected employee cannot perform every requested service");
        }
        if (!scheduleAvailability.isEmployeeAvailable(
                employee.getId(),
                booking.getSalon(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTotalDurationMinutes()
        )) {
            return ApiResponse.error("Selected employee is not working for the full requested appointment");
        }
        if (booking.getStatus() == BookingStatus.ACCEPTED
                && hasAcceptedBookingConflict(employee.getId(), booking)) {
            return ApiResponse.error("Selected employee already has an accepted appointment during this time");
        }

        String normalizedOwnerNote = normalizeText(ownerNote);
        Long previousEmployeeId = booking.getAssignedEmployee() == null
                ? null
                : booking.getAssignedEmployee().getId();
        boolean changingAssignedEmployee = previousEmployeeId != null
                && previousEmployeeId != employee.getId();
        if (changingAssignedEmployee && normalizedOwnerNote == null) {
            return ApiResponse.error("Please explain the employee change to the customer");
        }
        if (normalizedOwnerNote != null && normalizedOwnerNote.length() > MAX_NOTE_LENGTH) {
            return ApiResponse.error("Owner note cannot exceed " + MAX_NOTE_LENGTH + " characters");
        }

        booking.setAssignedEmployee(employee);
        if (normalizedOwnerNote != null) {
            booking.setOwnerNote(normalizedOwnerNote);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepo.save(booking);
        if (changingAssignedEmployee) {
            bookingNotificationService.bookingEmployeeReassigned(booking, previousEmployeeId);
        }

        return ApiResponse.success("Employee assigned successfully", mapToResponse(booking));
    }

    private ApiResponse<Void> validateCreateRequest(CreateBookingRequest request) {
        if (request.salonId() == null) {
            return ApiResponse.error("Salon id is required");
        }
        if (request.serviceIds() == null || request.serviceIds().isEmpty()) {
            return ApiResponse.error("At least one service is required");
        }
        if (request.serviceIds().size() > MAX_SERVICES_PER_BOOKING) {
            return ApiResponse.error("A booking cannot contain more than 10 services");
        }
        if (request.serviceIds().stream().anyMatch(id -> id == null)) {
            return ApiResponse.error("Every service id is required");
        }
        if (request.customerLocation() == null || request.customerLocation().isBlank()) {
            return ApiResponse.error("Customer location is required");
        }
        if (request.customerLocation().trim().length() > MAX_LOCATION_LENGTH) {
            return ApiResponse.error("Customer location cannot exceed 255 characters");
        }
        if (request.note() != null && request.note().trim().length() > MAX_NOTE_LENGTH) {
            return ApiResponse.error("Booking note cannot exceed 1000 characters");
        }
        if (request.bookingDate() == null || request.bookingTime() == null) {
            return ApiResponse.error("Booking date and time are required");
        }
        if (!LocalDateTime.of(request.bookingDate(), request.bookingTime()).isAfter(LocalDateTime.now())) {
            return ApiResponse.error("Booking date and time must be in the future");
        }
        return ApiResponse.success("Booking information is valid", null);
    }

    private ApiResponse<Void> validateOwnerAction(BookingActionRequest request, boolean noteRequired) {
        String ownerNote = request == null ? null : normalizeText(request.ownerNote());
        if (noteRequired && ownerNote == null) {
            return ApiResponse.error("A rejection reason is required");
        }
        if (ownerNote != null && ownerNote.length() > MAX_NOTE_LENGTH) {
            return ApiResponse.error("Owner note cannot exceed 1000 characters");
        }
        return ApiResponse.success("Booking action is valid", null);
    }

    private Booking findOwnedBookingForUpdate(Long bookingId, Long ownerId) {
        if (bookingId == null || ownerId == null || findManageableSalon(ownerId) == null) {
            return null;
        }
        Booking booking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null
                || booking.getSalon().getOwner() == null
                || booking.getSalon().getOwner().getId() != ownerId) {
            return null;
        }
        return booking;
    }

    private Salon findManageableSalon(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return isActiveSalon(salon) ? salon : null;
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepo.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.Active)
                .orElse(null);
    }

    private User findAvailableSalonEmployee(Long employeeId, Long salonId) {
        User employee = findActiveUser(employeeId);
        return isAvailableSalonEmployee(employee, salonId) ? employee : null;
    }

    private User findAvailableSalonEmployeeForUpdate(Long employeeId, Long salonId) {
        if (employeeId == null) {
            return null;
        }
        User employee = userRepo.findByIdForUpdate(employeeId)
                .filter(user -> user.getStatus() == UserStatus.Active)
                .orElse(null);
        return isAvailableSalonEmployee(employee, salonId) ? employee : null;
    }

    private boolean isAvailableSalonEmployee(User employee, Long salonId) {
        if (employee == null
                || employee.getRole() != UserRole.Employee
                || employee.getSalon() == null
                || salonId == null
                || !employee.getSalon().getId().equals(salonId)) {
            return false;
        }
        return true;
    }

    private boolean hasAcceptedBookingConflict(Long employeeId, Booking targetBooking) {
        LocalDateTime targetStart = LocalDateTime.of(
                targetBooking.getBookingDate(),
                targetBooking.getBookingTime()
        );
        LocalDateTime targetEnd = targetStart.plusMinutes(targetBooking.getTotalDurationMinutes());

        return bookingRepo.findByAssignedEmployeeIdAndBookingDateAndStatus(
                        employeeId,
                        targetBooking.getBookingDate(),
                        BookingStatus.ACCEPTED
                ).stream()
                .filter(existing -> targetBooking.getId() == null
                        || !targetBooking.getId().equals(existing.getId()))
                .anyMatch(existing -> {
                    LocalDateTime existingStart = LocalDateTime.of(
                            existing.getBookingDate(),
                            existing.getBookingTime()
                    );
                    LocalDateTime existingEnd = existingStart.plusMinutes(existing.getTotalDurationMinutes());
                    return existingStart.isBefore(targetEnd) && targetStart.isBefore(existingEnd);
                });
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private boolean canPerformAllBookingServices(Long employeeId, Booking booking) {
        Set<Long> serviceIds = booking.getServiceItems().stream()
                .map(BookingServiceItem::getSourceServiceId)
                .collect(java.util.stream.Collectors.toSet());
        return canPerformAllServices(employeeId, serviceIds);
    }

    private boolean canPerformAllServices(Long employeeId, Set<Long> serviceIds) {
        if (employeeId == null || serviceIds == null || serviceIds.isEmpty()) {
            return false;
        }
        return capabilityRepo.countByEmployeeIdAndServiceIdIn(employeeId, serviceIds)
                == serviceIds.size();
    }

    private List<BookingResponse> mapBookings(List<Booking> bookings) {
        return bookings.stream().map(this::mapToResponse).toList();
    }

    private BookingResponse mapToResponse(Booking booking) {
        User customer = booking.getCustomer();
        User employee = booking.getAssignedEmployee();
        List<BookingServiceItemResponse> services = booking.getServiceItems().stream()
                .map(item -> new BookingServiceItemResponse(
                        item.getId(),
                        item.getSourceServiceId(),
                        item.getServiceName(),
                        item.getPrice(),
                        item.getDurationMinutes()
                ))
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getSalon().getId(),
                booking.getSalon().getName(),
                booking.getSalon().getLocation(),
                customer.getId(),
                fullName(customer),
                customer.getEmail(),
                booking.getCustomerPhoneNumber(),
                booking.getCustomerLocation(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getStatus(),
                services,
                booking.getTotalPrice(),
                booking.getTotalDurationMinutes(),
                employee == null ? null : employee.getId(),
                employee == null ? null : fullName(employee),
                employee == null ? null : employeeSpecialty(employee),
                booking.getNote(),
                booking.getOwnerNote(),
                booking.getOwnerSuggestedTime(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private String employeeSpecialty(User employee) {
        return employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                ? null
                : employee.getCareerSkills().get(0);
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserName() : fullName;
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
