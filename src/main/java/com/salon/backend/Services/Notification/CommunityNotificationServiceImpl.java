package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.community.Friendship;
import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import com.salon.backend.Services.Email.NotificationEmailBridge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommunityNotificationServiceImpl implements CommunityNotificationService {

    private static final String COMMUNITY_PATH = "/community";
    private final UserNotificationRepo notificationRepo;
    private final NotificationEmailBridge emailBridge;

    @Override
    public void friendRequestReceived(Friendship friendship) {
        save(
                friendship,
                friendship.getAddressee(),
                NotificationEventType.FRIEND_REQUEST_RECEIVED,
                "received",
                displayName(friendship.getRequester()) + " sent you a friend request."
        );
    }

    @Override
    public void friendRequestAccepted(Friendship friendship) {
        save(
                friendship,
                friendship.getRequester(),
                NotificationEventType.FRIEND_REQUEST_ACCEPTED,
                "accepted",
                displayName(friendship.getAddressee()) + " accepted your friend request."
        );
    }

    @Override
    public void friendRequestDeclined(Friendship friendship) {
        save(
                friendship,
                friendship.getRequester(),
                NotificationEventType.FRIEND_REQUEST_DECLINED,
                "declined",
                displayName(friendship.getAddressee()) + " declined your friend request."
        );
    }

    private void save(
            Friendship friendship,
            User recipient,
            NotificationEventType eventType,
            String eventSuffix,
            String message) {
        if (friendship == null || friendship.getId() == null || recipient == null) return;
        String eventKey = "friendship:" + friendship.getId() + ":" + eventSuffix;
        if (notificationRepo.existsByEventKey(eventKey)) return;

        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setDomain(NotificationDomain.COMMUNITY);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setTargetPath(COMMUNITY_PATH);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepo.save(notification);
        emailBridge.notificationSaved(notification);
    }

    private String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? user.getUserName() : name;
    }
}
