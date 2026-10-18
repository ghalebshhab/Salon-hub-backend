package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.BookingRescheduleProposalRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRescheduleManagementImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-18T09:00:00Z"),
            ZoneId.of("Asia/Amman")
    );

    @Mock private BookingRepo bookingRepo;
    @Mock private BookingRescheduleProposalRepo proposalRepo;
    @Mock private UserRepo userRepo;
    @Mock private BookingAvailabilityService availabilityService;
    @Mock private BookingNotificationService notificationService;

    private BookingRescheduleManagementImpl rescheduleManagement;

    @BeforeEach
    void setUp() {
        rescheduleManagement = new BookingRescheduleManagementImpl(
                bookingRepo,
                proposalRepo,
                userRepo,
                availabilityService,
                notificationService,
                CLOCK
        );
    }

    @Test
    void ownerProposalKeepsOriginalAppointmentUntilCustomerDecision() {
        Booking booking = booking();
        BookingRescheduleProposalRequest request = request();
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(proposalRepo.existsByBookingIdAndStatus(30L, BookingRescheduleStatus.PENDING))
                .thenReturn(false);
        when(availabilityService.isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        )).thenReturn(true);
        when(proposalRepo.save(any())).thenAnswer(invocation -> {
            BookingRescheduleProposal proposal = invocation.getArgument(0);
            proposal.setId(50L);
            return proposal;
        });

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.propose(
                30L,
                request,
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(LocalDate.of(2026, 10, 20), booking.getBookingDate());
        assertEquals(LocalTime.of(10, 0), booking.getBookingTime());
        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
        assertEquals(BookingRescheduleStatus.PENDING, response.getData().status());
        verify(notificationService).bookingRescheduleProposed(any(), any());
    }

    @Test
    void anotherOwnerCannotProposeAReschedule() {
        Booking booking = booking();
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.propose(
                30L,
                request(),
                9L
        );

        assertFalse(response.isSuccess());
        verify(availabilityService, never()).isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        );
        verify(proposalRepo, never()).save(any());
    }

    @Test
    void customerAcceptanceRevalidatesAndMovesAppointment() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = proposal(booking, BookingRescheduleStatus.PENDING);
        arrangeCustomerProposal(proposal);
        when(availabilityService.isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        )).thenReturn(true);

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.accept(
                30L,
                50L,
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals(LocalDate.of(2026, 10, 21), booking.getBookingDate());
        assertEquals(LocalTime.of(14, 0), booking.getBookingTime());
        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
        assertEquals(BookingRescheduleStatus.ACCEPTED, proposal.getStatus());
        verify(notificationService).bookingRescheduleAccepted(booking, proposal);
    }

    @Test
    void unavailableProposalRemainsPendingAndDoesNotMoveAppointment() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = proposal(booking, BookingRescheduleStatus.PENDING);
        arrangeCustomerProposal(proposal);
        when(availabilityService.isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        )).thenReturn(false);

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.accept(
                30L,
                50L,
                1L
        );

        assertFalse(response.isSuccess());
        assertEquals(LocalDate.of(2026, 10, 20), booking.getBookingDate());
        assertEquals(LocalTime.of(10, 0), booking.getBookingTime());
        assertEquals(BookingRescheduleStatus.PENDING, proposal.getStatus());
        verify(notificationService, never()).bookingRescheduleAccepted(any(), any());
    }

    @Test
    void customerRejectionPreservesOriginalAppointment() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = proposal(booking, BookingRescheduleStatus.PENDING);
        arrangeCustomerProposal(proposal);

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.reject(
                30L,
                50L,
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals(LocalDate.of(2026, 10, 20), booking.getBookingDate());
        assertEquals(LocalTime.of(10, 0), booking.getBookingTime());
        assertEquals(BookingRescheduleStatus.REJECTED, proposal.getStatus());
        verify(notificationService).bookingRescheduleRejected(booking, proposal);
    }

    @Test
    void anotherCustomerCannotReadOrDecideProposal() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = proposal(booking, BookingRescheduleStatus.PENDING);
        User anotherCustomer = user(9L);
        when(userRepo.findById(9L)).thenReturn(Optional.of(anotherCustomer));
        when(proposalRepo.findOwnedProposalForUpdate(50L, 30L))
                .thenReturn(Optional.of(proposal));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.accept(
                30L,
                50L,
                9L
        );

        assertFalse(response.isSuccess());
        assertEquals(BookingRescheduleStatus.PENDING, proposal.getStatus());
        verify(availabilityService, never()).isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        );
    }

    @Test
    void repeatedAcceptReturnsTheAcceptedProposalWithoutAnotherMutation() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = proposal(booking, BookingRescheduleStatus.ACCEPTED);
        arrangeCustomerProposal(proposal);

        ApiResponse<BookingRescheduleProposalResponse> response = rescheduleManagement.accept(
                30L,
                50L,
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals("Reschedule proposal was already accepted", response.getMessage());
        verify(availabilityService, never()).isSlotAvailable(
                any(), any(), any(), any(), anyInt(), any(), anyLong()
        );
        verify(notificationService, never()).bookingRescheduleAccepted(any(), any());
    }

    private void arrangeCustomerProposal(BookingRescheduleProposal proposal) {
        when(userRepo.findById(1L)).thenReturn(Optional.of(proposal.getBooking().getCustomer()));
        when(proposalRepo.findOwnedProposalForUpdate(50L, 30L))
                .thenReturn(Optional.of(proposal));
        when(bookingRepo.findByIdForUpdate(30L))
                .thenReturn(Optional.of(proposal.getBooking()));
    }

    private BookingRescheduleProposalRequest request() {
        return new BookingRescheduleProposalRequest(
                LocalDate.of(2026, 10, 21),
                LocalTime.of(14, 0),
                "Would this time work for you?"
        );
    }

    private BookingRescheduleProposal proposal(
            Booking booking,
            BookingRescheduleStatus status) {
        BookingRescheduleProposal proposal = new BookingRescheduleProposal();
        proposal.setId(50L);
        proposal.setBooking(booking);
        proposal.setProposedBy(booking.getSalon().getOwner());
        proposal.setProposedDate(LocalDate.of(2026, 10, 21));
        proposal.setProposedTime(LocalTime.of(14, 0));
        proposal.setStatus(status);
        proposal.setCreatedAt(LocalDateTime.now(CLOCK));
        return proposal;
    }

    private Booking booking() {
        User customer = user(1L);
        User owner = user(2L);
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Salon Test");
        salon.setStatus(SalonStatus.Accepted);
        salon.setOwner(owner);

        BookingServiceItem serviceItem = new BookingServiceItem();
        serviceItem.setSourceServiceId(20L);
        serviceItem.setDurationMinutes(30);

        Booking booking = new Booking();
        booking.setId(30L);
        booking.setCustomer(customer);
        booking.setSalon(salon);
        booking.setBookingDate(LocalDate.of(2026, 10, 20));
        booking.setBookingTime(LocalTime.of(10, 0));
        booking.setTotalDurationMinutes(30);
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setCreatedAt(LocalDateTime.now(CLOCK));
        booking.setUpdatedAt(LocalDateTime.now(CLOCK));
        booking.addServiceItem(serviceItem);
        return booking;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.Active);
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        return user;
    }
}
