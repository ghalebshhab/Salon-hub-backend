package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingActionRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingManagementImplTest {

    @Mock private BookingRepo bookingRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private SalonServiceRepo salonServiceRepo;
    @Mock private UserRepo userRepo;
    @Mock private EmployeeServiceCapabilityRepo capabilityRepo;
    @Mock private EmployeeScheduleAvailability scheduleAvailability;
    @Mock private BookingNotificationService bookingNotificationService;
    @Mock private BookingAvailabilityService bookingAvailabilityService;
    @InjectMocks private BookingManagementImpl bookingManagement;

    @BeforeEach
    void allowWorkingHoursUnlessATestOverridesThem() {
        lenient().when(scheduleAvailability.isSalonOpenFor(any(), any(), anyInt()))
                .thenReturn(true);
        lenient().when(scheduleAvailability.isEmployeeAvailable(
                any(Long.class), any(), any(), any(), anyInt()
        ))
                .thenReturn(true);
        lenient().when(bookingAvailabilityService.isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any()
        )).thenReturn(true);
    }

    @Test
    void createsPendingBookingWithImmutableServiceSnapshotAndProfilePhoneFallback() {
        User customer = activeUser(1L, UserRole.USER, null);
        customer.setPhoneNumber("0790000000");
        Salon salon = activeSalon(10L, 2L);
        SalonService haircut = salonService(20L, salon, "Haircut", "7.50", 30);
        SalonService color = salonService(21L, salon, "Color", "12.00", 45);
        CreateBookingRequest request = new CreateBookingRequest(
                salon.getId(),
                List.of(20L, 21L),
                null,
                null,
                "Amman",
                LocalDateTime.now().plusDays(1).toLocalDate(),
                LocalDateTime.now().plusDays(1).toLocalTime(),
                "  Please call first  "
        );

        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L, 21L), 10L))
                .thenReturn(List.of(haircut, color));

        ApiResponse<BookingResponse> response = bookingManagement.createBooking(request, 1L);

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.PENDING, response.getData().status());
        assertEquals(new BigDecimal("19.50"), response.getData().totalPrice());
        assertEquals(75, response.getData().totalDurationMinutes());
        assertEquals("0790000000", response.getData().customerPhoneNumber());
        assertEquals("Please call first", response.getData().note());
        assertEquals(List.of("Haircut", "Color"), response.getData().services().stream()
                .map(item -> item.name()).toList());
        verify(bookingRepo).save(any(Booking.class));
    }

    @Test
    void rejectsBookingWhenAnySelectedServiceIsInactiveOrFromAnotherSalon() {
        User customer = activeUser(1L, UserRole.USER, null);
        Salon salon = activeSalon(10L, 2L);
        CreateBookingRequest request = validCreateRequest(10L, List.of(20L, 21L));

        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L, 21L), 10L))
                .thenReturn(List.of(salonService(20L, salon, "Haircut", "7.50", 30)));

        ApiResponse<BookingResponse> response = bookingManagement.createBooking(request, 1L);

        assertFalse(response.isSuccess());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void rejectsDuplicateServicesBeforeCreatingBooking() {
        User customer = activeUser(1L, UserRole.USER, null);
        Salon salon = activeSalon(10L, 2L);
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));

        ApiResponse<BookingResponse> response = bookingManagement.createBooking(
                validCreateRequest(10L, List.of(20L, 20L)),
                1L
        );

        assertFalse(response.isSuccess());
        verify(salonServiceRepo, never()).findByIdInAndSalonIdAndActiveTrue(any(), any());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void rejectsBookingWhenTheSelectedSlotIsNoLongerAvailable() {
        User customer = activeUser(1L, UserRole.USER, null);
        Salon salon = activeSalon(10L, 2L);
        SalonService service = salonService(20L, salon, "Haircut", "7.50", 30);
        CreateBookingRequest request = validCreateRequest(10L, List.of(20L));

        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L), 10L))
                .thenReturn(List.of(service));
        when(bookingAvailabilityService.isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any()
        )).thenReturn(false);

        ApiResponse<BookingResponse> response = bookingManagement.createBooking(request, 1L);

        assertFalse(response.isSuccess());
        assertEquals("Selected appointment time is no longer available", response.getMessage());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void onlyOwningSalonCanAcceptBooking() {
        Salon ownerSalon = activeSalon(10L, 2L);
        Booking booking = booking(30L, ownerSalon, activeUser(1L, UserRole.USER, null));
        when(salonRepo.findByOwnerId(3L)).thenReturn(activeSalon(11L, 3L));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingResponse> response = bookingManagement.acceptBooking(30L, null, 3L);

        assertFalse(response.isSuccess());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void ownerCanAcceptAndAssignAnEmployeeFromTheSameSalon() {
        Salon salon = activeSalon(10L, 2L);
        User employee = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);

        ApiResponse<BookingResponse> response = bookingManagement.acceptBooking(
                30L,
                new BookingActionRequest("Confirmed", null, 4L),
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.ACCEPTED, response.getData().status());
        assertEquals(4L, response.getData().assignedEmployeeId());
        verify(bookingRepo).save(booking);
        verify(bookingNotificationService).bookingAccepted(booking);
    }

    @Test
    void ownerCannotAcceptOverlappingBookingForTheSameEmployee() {
        Salon salon = activeSalon(10L, 2L);
        User employee = activeUser(4L, UserRole.Employee, salon);
        Booking requestedBooking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        Booking existingBooking = booking(31L, salon, activeUser(5L, UserRole.USER, null));
        existingBooking.setStatus(BookingStatus.ACCEPTED);
        existingBooking.setAssignedEmployee(employee);
        existingBooking.setBookingDate(requestedBooking.getBookingDate());
        existingBooking.setBookingTime(requestedBooking.getBookingTime().plusMinutes(15));

        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(requestedBooking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);
        when(bookingRepo.findByAssignedEmployeeIdAndBookingDateAndStatus(
                4L,
                requestedBooking.getBookingDate(),
                BookingStatus.ACCEPTED
        )).thenReturn(List.of(existingBooking));

        ApiResponse<BookingResponse> response = bookingManagement.acceptBooking(
                30L,
                new BookingActionRequest("Confirmed", null, 4L),
                2L
        );

        assertFalse(response.isSuccess());
        assertEquals(
                "Selected employee already has an accepted appointment during this time",
                response.getMessage()
        );
        assertEquals(BookingStatus.PENDING, requestedBooking.getStatus());
        verify(bookingRepo, never()).save(any());
        verify(bookingNotificationService, never()).bookingAccepted(any());
    }

    @Test
    void adjacentAcceptedBookingDoesNotCreateAConflict() {
        Salon salon = activeSalon(10L, 2L);
        User employee = activeUser(4L, UserRole.Employee, salon);
        Booking requestedBooking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        Booking existingBooking = booking(31L, salon, activeUser(5L, UserRole.USER, null));
        existingBooking.setStatus(BookingStatus.ACCEPTED);
        existingBooking.setAssignedEmployee(employee);
        existingBooking.setBookingDate(requestedBooking.getBookingDate());
        existingBooking.setBookingTime(requestedBooking.getBookingTime().plusMinutes(30));

        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(requestedBooking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);
        when(bookingRepo.findByAssignedEmployeeIdAndBookingDateAndStatus(
                4L,
                requestedBooking.getBookingDate(),
                BookingStatus.ACCEPTED
        )).thenReturn(List.of(existingBooking));

        ApiResponse<BookingResponse> response = bookingManagement.acceptBooking(
                30L,
                new BookingActionRequest("Confirmed", null, 4L),
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.ACCEPTED, requestedBooking.getStatus());
        verify(bookingRepo).save(requestedBooking);
    }

    @Test
    void ownerCannotAssignEmployeeMissingARequestedServiceCapability() {
        Salon salon = activeSalon(10L, 2L);
        User employee = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(0L);

        ApiResponse<BookingResponse> response = bookingManagement.assignEmployee(30L, 4L, 2L);

        assertFalse(response.isSuccess());
        assertNull(booking.getAssignedEmployee());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void ownerCanReassignEmployeeAndSaveCustomerUpdate() {
        Salon salon = activeSalon(10L, 2L);
        User previousEmployee = activeUser(3L, UserRole.Employee, salon);
        User replacement = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(previousEmployee);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(replacement));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);

        ApiResponse<BookingResponse> response = bookingManagement.assignEmployee(
                30L,
                4L,
                "Your selected employee is unavailable. Alex will help you instead.",
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(4L, response.getData().assignedEmployeeId());
        assertEquals(
                "Your selected employee is unavailable. Alex will help you instead.",
                response.getData().ownerNote()
        );
        verify(bookingRepo).save(booking);
        verify(bookingNotificationService).bookingEmployeeReassigned(booking, 3L);
    }

    @Test
    void reassignmentRequiresAnUpdateForTheCustomer() {
        Salon salon = activeSalon(10L, 2L);
        User previousEmployee = activeUser(3L, UserRole.Employee, salon);
        User replacement = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(previousEmployee);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(replacement));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);

        ApiResponse<BookingResponse> response = bookingManagement.assignEmployee(
                30L,
                4L,
                "  ",
                2L
        );

        assertFalse(response.isSuccess());
        assertEquals(3L, booking.getAssignedEmployee().getId());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void acceptedBookingCannotBeReassignedToAnEmployeeWithAConflict() {
        Salon salon = activeSalon(10L, 2L);
        User previousEmployee = activeUser(3L, UserRole.Employee, salon);
        User replacement = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(previousEmployee);
        Booking conflict = booking(31L, salon, activeUser(5L, UserRole.USER, null));
        conflict.setStatus(BookingStatus.ACCEPTED);
        conflict.setAssignedEmployee(replacement);
        conflict.setBookingDate(booking.getBookingDate());
        conflict.setBookingTime(booking.getBookingTime().plusMinutes(10));

        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(replacement));
        when(capabilityRepo.countByEmployeeIdAndServiceIdIn(4L, java.util.Set.of(20L)))
                .thenReturn(1L);
        when(bookingRepo.findByAssignedEmployeeIdAndBookingDateAndStatus(
                4L,
                booking.getBookingDate(),
                BookingStatus.ACCEPTED
        )).thenReturn(List.of(conflict));

        ApiResponse<BookingResponse> response = bookingManagement.assignEmployee(
                30L,
                4L,
                "Another employee will take your appointment.",
                2L
        );

        assertFalse(response.isSuccess());
        assertEquals(3L, booking.getAssignedEmployee().getId());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void bookingOutsideSalonHoursIsRejectedBeforePersistence() {
        User customer = activeUser(1L, UserRole.USER, null);
        Salon salon = activeSalon(10L, 2L);
        SalonService haircut = salonService(20L, salon, "Haircut", "7.50", 30);
        CreateBookingRequest request = validCreateRequest(10L, List.of(20L));
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L), 10L))
                .thenReturn(List.of(haircut));
        when(scheduleAvailability.isSalonOpenFor(salon, request.bookingTime(), 30))
                .thenReturn(false);

        ApiResponse<BookingResponse> response = bookingManagement.createBooking(request, 1L);

        assertFalse(response.isSuccess());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void rejectionRequiresReasonAndLeavesBookingPendingWhenMissing() {
        Salon salon = activeSalon(10L, 2L);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingResponse> response = bookingManagement.rejectBooking(
                30L,
                new BookingActionRequest("  ", null, null),
                2L
        );

        assertFalse(response.isSuccess());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void successfulRejectionCreatesCustomerNotification() {
        Salon salon = activeSalon(10L, 2L);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingResponse> response = bookingManagement.rejectBooking(
                30L,
                new BookingActionRequest("This time is unavailable", null, null),
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.REJECTED, booking.getStatus());
        verify(bookingNotificationService).bookingRejected(booking);
    }

    @Test
    void customerCanCancelAnAcceptedBookingButCannotCancelAgain() {
        Salon salon = activeSalon(10L, 2L);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        booking.setStatus(BookingStatus.ACCEPTED);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingResponse> first = bookingManagement.cancelBooking(30L, 1L);
        ApiResponse<BookingResponse> second = bookingManagement.cancelBooking(30L, 1L);

        assertTrue(first.isSuccess());
        assertEquals(BookingStatus.CANCELLED, first.getData().status());
        assertFalse(second.isSuccess());
        verify(bookingRepo).save(booking);
        verify(bookingNotificationService).bookingCancelled(booking);
    }

    @Test
    void completingAcceptedBookingPreservesTheAssignedEmployeeAndSnapshots() {
        Salon salon = activeSalon(10L, 2L);
        User employee = activeUser(4L, UserRole.Employee, salon);
        Booking booking = booking(30L, salon, activeUser(1L, UserRole.USER, null));
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(employee);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingResponse> response = bookingManagement.completeBooking(30L, 2L);

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.COMPLETED, response.getData().status());
        assertEquals(4L, response.getData().assignedEmployeeId());
        assertEquals("Haircut", response.getData().services().get(0).name());
        assertNull(response.getData().ownerSuggestedTime());
    }

    private CreateBookingRequest validCreateRequest(Long salonId, List<Long> serviceIds) {
        LocalDateTime appointment = LocalDateTime.now().plusDays(1);
        return new CreateBookingRequest(
                salonId,
                serviceIds,
                null,
                "0790000000",
                "Amman",
                appointment.toLocalDate(),
                appointment.toLocalTime(),
                null
        );
    }

    private User activeUser(Long id, UserRole role, Salon salon) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUserName("test" + id);
        user.setEmail("test" + id + "@example.com");
        user.setStatus(UserStatus.Active);
        user.setRole(role);
        user.setSalon(salon);
        user.setCareerSkills(new ArrayList<>());
        return user;
    }

    private Salon activeSalon(Long id, Long ownerId) {
        User owner = activeUser(ownerId, UserRole.OWNER, null);
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName("Salon " + id);
        salon.setLocation("Amman");
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        owner.setSalon(salon);
        return salon;
    }

    private SalonService salonService(
            Long id,
            Salon salon,
            String name,
            String price,
            int duration) {
        LocalDateTime now = LocalDateTime.now();
        return new SalonService(
                id,
                salon,
                name,
                null,
                new BigDecimal(price),
                duration,
                true,
                now,
                now
        );
    }

    private Booking booking(Long id, Salon salon, User customer) {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setSalon(salon);
        booking.setCustomerPhoneNumber("0790000000");
        booking.setCustomerLocation("Amman");
        booking.setBookingDate(now.plusDays(1).toLocalDate());
        booking.setBookingTime(now.plusDays(1).toLocalTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(new BigDecimal("7.50"));
        booking.setTotalDurationMinutes(30);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        BookingServiceItem item = new BookingServiceItem();
        item.setId(40L);
        item.setSourceServiceId(20L);
        item.setServiceName("Haircut");
        item.setPrice(new BigDecimal("7.50"));
        item.setDurationMinutes(30);
        booking.addServiceItem(item);
        return booking;
    }
}
