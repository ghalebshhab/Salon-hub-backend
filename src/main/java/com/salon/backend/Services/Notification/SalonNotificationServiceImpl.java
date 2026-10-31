package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SalonNotificationServiceImpl implements SalonNotificationService {

    private static final String MY_SALON_PATH = "/owner/my-salon";
    private final UserNotificationRepo notificationRepo;

    @Override
    public void notifyRequestSubmitted(Salon salon) {
        save(
                salon,
                NotificationEventType.SALON_REQUEST_SUBMITTED,
                "submitted:0",
                "Your salon request for \"" + salon.getName()
                        + "\" was submitted and is waiting for admin review.",
                null
        );
    }

    @Override
    public void notifyRequestResubmitted(Salon salon) {
        save(
                salon,
                NotificationEventType.SALON_REQUEST_RESUBMITTED,
                "resubmitted:" + salon.getResubmissionCount(),
                "Your updated salon request for \"" + salon.getName()
                        + "\" was resubmitted and is waiting for admin review.",
                null
        );
    }

    @Override
    public void notifyRequestAccepted(Salon salon) {
        boolean acceptedAfterEdit = salon.getResubmissionCount() > 0;
        save(
                salon,
                NotificationEventType.SALON_REQUEST_ACCEPTED,
                "accepted:" + salon.getResubmissionCount(),
                acceptedAfterEdit
                        ? "Your updated salon request for \"" + salon.getName()
                                + "\" was accepted after review. Your salon is now active."
                        : "Your salon request for \"" + salon.getName()
                                + "\" was accepted. Your salon is now active.",
                null
        );
    }

    @Override
    public void notifyRequestRejected(Salon salon, String rejectionReason) {
        String normalizedReason = rejectionReason == null ? null : rejectionReason.trim();
        save(
                salon,
                NotificationEventType.SALON_REQUEST_REJECTED,
                "rejected:" + salon.getResubmissionCount(),
                "Your salon request for \"" + salon.getName()
                        + "\" was rejected. Reason: " + normalizedReason,
                normalizedReason
        );
    }

    @Override
    public void notifySalonBlocked(Salon salon) {
        save(
                salon,
                NotificationEventType.SALON_BLOCKED,
                "blocked:" + versionKey(salon),
                "Your salon \"" + salon.getName()
                        + "\" was blocked by an administrator. Owner access is temporarily disabled.",
                null
        );
    }

    @Override
    public void notifySalonUnblocked(Salon salon) {
        save(
                salon,
                NotificationEventType.SALON_UNBLOCKED,
                "unblocked:" + versionKey(salon),
                "Your salon \"" + salon.getName()
                        + "\" was unblocked. Your salon data and owner access have been restored.",
                null
        );
    }

    private void save(
            Salon salon,
            NotificationEventType eventType,
            String eventSuffix,
            String message,
            String reason) {
        if (salon == null || salon.getId() == null || salon.getOwner() == null) {
            return;
        }

        User recipient = salon.getOwner();
        String eventKey = "salon:" + salon.getId() + ":" + eventSuffix;
        if (notificationRepo.existsByEventKey(eventKey)) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setDomain(NotificationDomain.SALON);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setSalonId(salon.getId());
        notification.setSalonName(salon.getName());
        notification.setReason(reason);
        notification.setTargetPath(MY_SALON_PATH);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepo.save(notification);
    }

    private String versionKey(Salon salon) {
        return salon.getVersion() == null ? "new" : salon.getVersion().toString();
    }
}
