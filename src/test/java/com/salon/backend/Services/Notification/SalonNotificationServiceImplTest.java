package com.salon.backend.Services.Notification;

import com.salon.backend.Services.Email.NotificationEmailBridge;
import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalonNotificationServiceImplTest {

    @Mock private UserNotificationRepo notificationRepo;
    @Mock private NotificationEmailBridge emailBridge;
    @InjectMocks private SalonNotificationServiceImpl service;

    @Test
    void createsEverySalonLifecycleNotificationForTheOwner() {
        Salon salon = salon();

        service.notifyRequestSubmitted(salon);
        salon.setResubmissionCount(1);
        service.notifyRequestResubmitted(salon);
        service.notifyRequestRejected(salon, "  Add a valid business license  ");
        service.notifyRequestAccepted(salon);
        service.notifySalonBlocked(salon);
        service.notifySalonUnblocked(salon);

        ArgumentCaptor<UserNotification> captor =
                ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(6)).save(captor.capture());
        List<UserNotification> notifications = captor.getAllValues();

        assertEquals(
                List.of(
                        NotificationEventType.SALON_REQUEST_SUBMITTED,
                        NotificationEventType.SALON_REQUEST_RESUBMITTED,
                        NotificationEventType.SALON_REQUEST_REJECTED,
                        NotificationEventType.SALON_REQUEST_ACCEPTED,
                        NotificationEventType.SALON_BLOCKED,
                        NotificationEventType.SALON_UNBLOCKED
                ),
                notifications.stream().map(UserNotification::getEventType).toList()
        );
        notifications.forEach(notification -> {
            assertEquals(NotificationDomain.SALON, notification.getDomain());
            assertEquals(salon.getOwner(), notification.getRecipient());
            assertEquals(salon.getId(), notification.getSalonId());
            assertEquals("/owner/my-salon", notification.getTargetPath());
        });
        UserNotification rejection = notifications.get(2);
        assertEquals("Add a valid business license", rejection.getReason());
        assertTrue(rejection.getMessage().contains(rejection.getReason()));
        assertTrue(notifications.get(3).getMessage().contains("updated salon request"));
    }

    private Salon salon() {
        User owner = new User();
        owner.setId(7L);

        Salon salon = new Salon();
        salon.setId(3L);
        salon.setVersion(4L);
        salon.setName("Plum Studio");
        salon.setOwner(owner);
        return salon;
    }
}
