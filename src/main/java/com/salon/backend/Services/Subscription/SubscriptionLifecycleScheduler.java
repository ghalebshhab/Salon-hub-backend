package com.salon.backend.Services.Subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Checks salon plans once an hour. The work lives in
 * {@link SubscriptionLifecycleService}; one failing salon never stops the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionLifecycleScheduler {

    private final SubscriptionLifecycleService lifecycleService;

    @Scheduled(
            initialDelayString = "${salon.subscription.lifecycle-initial-delay-ms:60000}",
            fixedDelayString = "${salon.subscription.lifecycle-delay-ms:3600000}")
    public void run() {
        int failed = 0;
        for (Long subscriptionId : lifecycleService.findOpenIds()) {
            try {
                lifecycleService.process(subscriptionId);
            } catch (RuntimeException exception) {
                failed++;
                log.warn("Could not update salon plan {}", subscriptionId, exception);
            }
        }
        if (failed > 0) {
            log.warn("{} salon plan(s) will be tried again in the next run", failed);
        }
    }
}
