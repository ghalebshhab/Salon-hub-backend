package com.salon.backend.Services.Salon.Booking.Departure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Once a minute, checks today's bookings whose customer turned on "Leave on
 * time" and sends the alerts that are due. The queue and the traffic are read
 * again on every run, so the leave time follows them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartureAlertScheduler {

    private final DeparturePlanService departurePlanService;

    @Scheduled(
            initialDelayString = "${salon.booking.departure-initial-delay-ms:45000}",
            fixedDelayString = "${salon.booking.departure-delay-ms:60000}")
    public void run() {
        for (Long bookingId : departurePlanService.findAlertCandidateIds()) {
            try {
                departurePlanService.sendDueAlerts(bookingId);
            } catch (RuntimeException exception) {
                log.warn("Could not check leave alerts for booking {}", bookingId, exception);
            }
        }
    }
}
