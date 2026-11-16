package com.salon.backend.Services.Salon.Booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the time-driven booking steps once a minute. The work itself lives in
 * {@link BookingLifecycleService}; this class only loops and logs failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycleScheduler {

    private final BookingLifecycleService lifecycleService;

    @Scheduled(
            initialDelayString = "${salon.booking.lifecycle-initial-delay-ms:30000}",
            fixedDelayString = "${salon.booking.lifecycle-delay-ms:60000}")
    public void run() {
        expireOverdueRequests();
        sendReminders();
    }

    void expireOverdueRequests() {
        int expired = 0;
        for (Long bookingId : lifecycleService.findOverduePendingIds()) {
            try {
                if (lifecycleService.expireIfOverdue(bookingId)) {
                    expired++;
                }
            } catch (RuntimeException exception) {
                log.warn("Could not expire booking {}", bookingId, exception);
            }
        }
        if (expired > 0) {
            log.info("Expired {} unanswered booking request(s)", expired);
        }
    }

    void sendReminders() {
        for (Long bookingId : lifecycleService.findReminderCandidateIds()) {
            try {
                lifecycleService.sendDueReminder(bookingId);
            } catch (RuntimeException exception) {
                log.warn("Could not send reminder for booking {}", bookingId, exception);
            }
        }
    }
}
