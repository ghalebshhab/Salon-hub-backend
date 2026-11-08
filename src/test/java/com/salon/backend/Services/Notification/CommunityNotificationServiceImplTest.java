package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.community.Friendship;
import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityNotificationServiceImplTest {

    @Mock private UserNotificationRepo notificationRepo;
    private CommunityNotificationService service;
    private Friendship friendship;

    @BeforeEach
    void setUp() {
        service = new CommunityNotificationServiceImpl(notificationRepo);
        User requester = user(11L, "Maya", "Stone", "maya");
        User addressee = user(22L, "Sara", "Lee", "sara");
        friendship = new Friendship();
        friendship.setId(41L);
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
    }

    @Test
    void createsInboxNotificationsForEveryFriendRequestTransition() {
        when(notificationRepo.existsByEventKey(any())).thenReturn(false);

        service.friendRequestReceived(friendship);
        service.friendRequestAccepted(friendship);
        service.friendRequestDeclined(friendship);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepo, times(3)).save(captor.capture());
        List<UserNotification> notifications = captor.getAllValues();

        assertNotification(notifications.get(0), 22L, NotificationEventType.FRIEND_REQUEST_RECEIVED,
                "friendship:41:received", "Maya Stone sent you a friend request.");
        assertNotification(notifications.get(1), 11L, NotificationEventType.FRIEND_REQUEST_ACCEPTED,
                "friendship:41:accepted", "Sara Lee accepted your friend request.");
        assertNotification(notifications.get(2), 11L, NotificationEventType.FRIEND_REQUEST_DECLINED,
                "friendship:41:declined", "Sara Lee declined your friend request.");
    }

    @Test
    void doesNotDuplicateAnExistingFriendNotification() {
        when(notificationRepo.existsByEventKey("friendship:41:received")).thenReturn(true);

        service.friendRequestReceived(friendship);

        verify(notificationRepo, never()).save(any());
    }

    private void assertNotification(UserNotification notification, Long recipientId,
                                    NotificationEventType eventType, String eventKey, String message) {
        assertEquals(recipientId, notification.getRecipient().getId());
        assertEquals(NotificationDomain.COMMUNITY, notification.getDomain());
        assertEquals(eventType, notification.getEventType());
        assertEquals(eventKey, notification.getEventKey());
        assertEquals(message, notification.getMessage());
        assertEquals("/community", notification.getTargetPath());
        assertNull(notification.getSalonId());
        assertNull(notification.getSalonName());
        assertNotNull(notification.getCreatedAt());
    }

    private User user(Long id, String firstName, String lastName, String userName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName(userName);
        return user;
    }
}
