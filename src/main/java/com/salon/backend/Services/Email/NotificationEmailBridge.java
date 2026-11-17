package com.salon.backend.Services.Email;

import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends the email copy of an in-app notification. The notification services
 * call it right after they save a notification, so salon, booking and
 * community events all reach the user's inbox by priority.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailBridge {

    private final EmailService emailService;
    private final EmailTemplates templates;

    public void notificationSaved(UserNotification notification) {
        if (notification == null || notification.getRecipient() == null) return;
        try {
            queueCopy(notification);
        } catch (RuntimeException exception) {
            // The email is a copy: it must never fail the action that made the notification.
            log.warn("Could not queue notification email: {}", exception.getClass().getSimpleName());
        }
    }

    private void queueCopy(UserNotification notification) {
        User recipient = notification.getRecipient();
        // Copy the values now: the email is sent after the transaction ends.
        EmailTemplate template = templates.forNotification(new EmailTemplates.NotificationEmailData(
                notification.getEventType(),
                notification.getMessage(),
                notification.getSalonName(),
                notification.getBookingDate(),
                notification.getBookingTime(),
                notification.getSuggestedDate(),
                notification.getSuggestedTime(),
                notification.getReason(),
                notification.getTargetPath()));
        emailService.queue(recipient.getEmail(), EmailNames.firstName(recipient), template);
    }
}
