package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.salons.Salon;

public interface SalonNotificationService {

    void notifyRequestSubmitted(Salon salon);

    void notifyRequestResubmitted(Salon salon);

    void notifyRequestAccepted(Salon salon);

    void notifyRequestRejected(Salon salon, String rejectionReason);

    void notifySalonBlocked(Salon salon);

    void notifySalonUnblocked(Salon salon);
}
