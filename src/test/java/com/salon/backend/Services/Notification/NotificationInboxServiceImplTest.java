package com.salon.backend.Services.Notification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Notification.NotificationPageResponse;
import com.salon.backend.DTOs.Notification.NotificationResponse;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.NotificationDomain;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationInboxServiceImplTest {

    @Mock private UserNotificationRepo notificationRepo;
    @InjectMocks private NotificationInboxServiceImpl notificationInboxService;

    @Test
    void listsOnlyAuthenticatedUsersNotificationsNewestFirst() {
        UserNotification notification = notification(10L, 2L);
        when(notificationRepo.findByRecipientId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        ApiResponse<NotificationPageResponse> response = notificationInboxService.getNotifications(
                2L,
                0,
                20
        );

        assertTrue(response.isSuccess());
        assertEquals(10L, response.getData().content().get(0).id());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepo).findByRecipientId(org.mockito.ArgumentMatchers.eq(2L), pageable.capture());
        assertEquals("createdAt: DESC,id: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void cannotMarkAnotherUsersNotificationAsRead() {
        when(notificationRepo.findOwnedByIdForUpdate(10L, 2L)).thenReturn(Optional.empty());

        ApiResponse<NotificationResponse> response = notificationInboxService.markAsRead(10L, 2L);

        assertFalse(response.isSuccess());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void marksOwnedNotificationAsReadOnce() {
        UserNotification notification = notification(10L, 2L);
        when(notificationRepo.findOwnedByIdForUpdate(10L, 2L))
                .thenReturn(Optional.of(notification));

        ApiResponse<NotificationResponse> response = notificationInboxService.markAsRead(10L, 2L);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().read());
        assertNotNull(response.getData().readAt());
        verify(notificationRepo).save(notification);
    }

    @Test
    void marksOnlyAuthenticatedUsersUnreadNotificationsAsRead() {
        ApiResponse<?> response = notificationInboxService.markAllAsRead(2L);

        assertTrue(response.isSuccess());
        ArgumentCaptor<LocalDateTime> readAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepo).markAllRead(org.mockito.ArgumentMatchers.eq(2L), readAt.capture());
        assertNotNull(readAt.getValue());
    }

    private UserNotification notification(Long notificationId, Long recipientId) {
        User recipient = new User();
        recipient.setId(recipientId);
        Salon salon = new Salon();
        salon.setId(3L);
        salon.setName("Salon Test");
        Booking booking = new Booking();
        booking.setId(4L);
        booking.setSalon(salon);

        UserNotification notification = new UserNotification();
        notification.setId(notificationId);
        notification.setRecipient(recipient);
        notification.setBooking(booking);
        notification.setDomain(NotificationDomain.BOOKING);
        notification.setSalonId(3L);
        notification.setSalonName("Salon Test");
        notification.setEventType(NotificationEventType.BOOKING_ACCEPTED);
        notification.setMessage("Accepted");
        notification.setBookingDate(LocalDate.now().plusDays(1));
        notification.setBookingTime(LocalTime.NOON);
        notification.setBookingStatus(BookingStatus.ACCEPTED);
        notification.setTargetPath("/my-bookings?bookingId=4");
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
