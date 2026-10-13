package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
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
    @InjectMocks private BookingNotificationServiceImpl bookingNotificationService;

    @Test
    void acceptedBookingWithSuggestedTimeCreatesBothCustomerEvents() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setOwnerSuggestedTime(LocalTime.of(14, 30));

        bookingNotificationService.bookingAccepted(booking);

        ArgumentCaptor<UserNotification> saved = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(2)).save(saved.capture());
        List<NotificationEventType> types = saved.getAllValues().stream()
                .map(UserNotification::getEventType)
                .toList();
        assertEquals(
                List.of(
                        NotificationEventType.BOOKING_ACCEPTED,
                        NotificationEventType.BOOKING_TIME_SUGGESTED
                ),
                types
        );
        assertTrue(saved.getAllValues().stream()
                .allMatch(notification -> notification.getRecipient().getId() == 1L));
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
    void existingEventKeyPreventsDuplicateNotification() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.ACCEPTED);
        when(notificationRepo.existsByEventKey("booking:20:accepted")).thenReturn(true);

        bookingNotificationService.bookingAccepted(booking);

        verify(notificationRepo, never()).save(any());
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
