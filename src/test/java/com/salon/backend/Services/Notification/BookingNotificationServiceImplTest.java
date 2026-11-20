package com.salon.backend.Services.Notification;

import com.salon.backend.Services.Subscription.PlanLimits;
import com.salon.backend.Services.Email.NotificationEmailBridge;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import com.salon.backend.Services.Salon.Booking.BookingTimePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingNotificationServiceImplTest {

    @Mock private UserNotificationRepo notificationRepo;
    @Mock private NotificationEmailBridge emailBridge;
    @Mock private PlanLimits planLimits;
    @InjectMocks private BookingNotificationServiceImpl bookingNotificationService;

    @Test
    void acceptedBookingCreatesCustomerEvent() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);

        bookingNotificationService.bookingAccepted(booking);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo).save(saved.capture());
        assertEquals(NotificationEventType.BOOKING_ACCEPTED, saved.getValue().getEventType());
        assertEquals(1L, saved.getValue().getRecipient().getId());
    }

    @Test
    void rejectedBookingCreatesCustomerEvent() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.REJECTED);

        bookingNotificationService.bookingRejected(booking);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo).save(saved.capture());
        assertEquals(NotificationEventType.BOOKING_REJECTED, saved.getValue().getEventType());
        assertEquals(1L, saved.getValue().getRecipient().getId());
    }

    @Test
    void reassignmentCreatesOneCustomerEventAndSameEmployeeDoesNot() {
        Booking booking = booking();
        User replacement = user(4L, "New", "Employee");
        booking.setAssignedEmployee(replacement);

        bookingNotificationService.bookingEmployeeReassigned(booking, 3L);
        bookingNotificationService.bookingEmployeeReassigned(booking, 4L);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo).save(saved.capture());
        assertEquals(
                NotificationEventType.BOOKING_EMPLOYEE_REASSIGNED,
                saved.getValue().getEventType()
        );
        assertEquals(1L, saved.getValue().getRecipient().getId());
    }

    @Test
    void customerCancellationNotifiesOwnerAndAssignedEmployee() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAssignedEmployee(user(4L, "Salon", "Employee"));

        bookingNotificationService.bookingCancelled(booking);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(2)).save(saved.capture());
        assertEquals(
                List.of(2L, 4L),
                saved.getAllValues().stream()
                        .map(notification -> notification.getRecipient().getId())
                        .toList()
        );
        assertEquals(
                List.of(
                        "/owner/bookings?bookingId=20",
                        "/employee/bookings?bookingId=20"
                ),
                saved.getAllValues().stream()
                        .map(UserNotification::getTargetPath)
                        .toList()
        );
    }

    @Test
    void anExpiredRequestTellsTheCustomerAndTheOwner() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.EXPIRED);

        bookingNotificationService.bookingExpired(booking);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(2)).save(saved.capture());
        assertEquals(
                List.of(1L, 2L),
                saved.getAllValues().stream().map(n -> n.getRecipient().getId()).toList()
        );
        assertTrue(saved.getAllValues().stream()
                .allMatch(n -> n.getEventType() == NotificationEventType.BOOKING_EXPIRED));
    }

    @Test
    void theDayReminderGoesOnlyToTheCustomerAndSaysTomorrow() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(user(4L, "Salon", "Employee"));

        bookingNotificationService.bookingReminder(
                booking, BookingTimePolicy.ReminderType.DAY, LocalDate.now());

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo).save(saved.capture());
        assertEquals(1L, saved.getValue().getRecipient().getId());
        assertEquals("booking:20:reminder:day:customer", saved.getValue().getEventKey());
        assertTrue(saved.getValue().getMessage().contains("tomorrow at 13:00"));
    }

    @Test
    void theSoonReminderAlsoTellsTheAssignedEmployee() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setAssignedEmployee(user(4L, "Salon", "Employee"));

        bookingNotificationService.bookingReminder(
                booking, BookingTimePolicy.ReminderType.SOON, booking.getBookingDate());

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(2)).save(saved.capture());
        assertEquals(
                List.of(1L, 4L),
                saved.getAllValues().stream().map(n -> n.getRecipient().getId()).toList()
        );
        assertTrue(saved.getAllValues().get(0).getMessage().contains("today at 13:00"));
    }

    @Test
    void existingEventKeyPreventsDuplicateNotification() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);
        when(notificationRepo.existsByEventKey("booking:20:accepted")).thenReturn(true);

        bookingNotificationService.bookingAccepted(booking);

        verify(notificationRepo, never()).save(any());
    }

    @Test
    void rescheduleProposalNotifiesCustomerWithProposedDateAndTime() {
        Booking booking = booking();
        BookingRescheduleProposal proposal = new BookingRescheduleProposal();
        proposal.setId(50L);
        proposal.setBooking(booking);
        proposal.setProposedDate(LocalDate.now().plusDays(2));
        proposal.setProposedTime(LocalTime.of(15, 30));
        proposal.setStatus(BookingRescheduleStatus.PENDING);

        bookingNotificationService.bookingRescheduleProposed(booking, proposal);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo).save(saved.capture());
        assertEquals(
                NotificationEventType.BOOKING_RESCHEDULE_PROPOSED,
                saved.getValue().getEventType()
        );
        assertEquals(booking.getCustomer().getId(), saved.getValue().getRecipient().getId());
        assertEquals(proposal.getProposedDate(), saved.getValue().getSuggestedDate());
        assertEquals(proposal.getProposedTime(), saved.getValue().getSuggestedTime());
    }

    private Booking booking() {
        User customer = user(1L, "Customer", "Name");
        User owner = user(2L, "Salon", "Owner");
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Salon Test");
        salon.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(20L);
        booking.setVersion(1L);
        booking.setCustomer(customer);
        booking.setSalon(salon);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setBookingTime(LocalTime.of(13, 0));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }

    private User user(Long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName("user" + id);
        return user;
    }
}
