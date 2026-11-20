package com.salon.backend.Services.Subscription;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.subscriptions.SalonSubscription;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentStatus;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Subscription.SalonSubscriptionRepo;
import com.salon.backend.Repositories.Subscription.SubscriptionPaymentRepo;
import com.salon.backend.Services.Email.EmailNames;
import com.salon.backend.Services.Email.EmailService;
import com.salon.backend.Services.Email.EmailTemplate;
import com.salon.backend.Services.Email.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

/**
 * Moves plans forward in time and tells the owner:
 * <ul>
 *   <li>Trial: emails 7 days and 1 day before the end, then ends on Free.</li>
 *   <li>Paid: emails 7 days and 1 day before the end, then GRACE for
 *       {@link SubscriptionPlan#GRACE_DAYS} days with a payment-due email.</li>
 *   <li>Grace: an email 1 day before the end, then ends on Free.</li>
 * </ul>
 * Each email is sent once; {@code lastNoticeKey} remembers the last one.
 * Reminders are skipped while the owner has a request waiting for review.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    static final int FIRST_REMINDER_DAYS = 7;
    static final int LAST_REMINDER_DAYS = 1;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final SalonSubscriptionRepo subscriptionRepo;
    private final SubscriptionPaymentRepo paymentRepo;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;
    private final Clock clock;

    /** Subscriptions that can change with time. */
    @Transactional(readOnly = true)
    public List<Long> findOpenIds() {
        return subscriptionRepo.findIdsByStatusIn(
                EnumSet.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE));
    }

    /** Applies the step that is due for one subscription. Safe to call again. */
    @Transactional
    public void process(Long subscriptionId) {
        SalonSubscription subscription = subscriptionRepo.findById(subscriptionId).orElse(null);
        if (subscription == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        switch (subscription.getStatus()) {
            case TRIAL -> processTrial(subscription, now);
            case ACTIVE -> processActive(subscription, now);
            case GRACE -> processGrace(subscription, now);
            case ENDED -> {
                // Nothing changes with time on Free.
            }
        }
    }

    private void processTrial(SalonSubscription subscription, LocalDateTime now) {
        LocalDateTime end = subscription.getTrialEndsAt();
        String planName = subscription.getPlan().getDisplayName();
        if (end == null || !now.isBefore(end)) {
            end(subscription, now, planName);
            return;
        }
        remind(subscription, now, "TRIAL", end,
                days -> emailTemplates.trialEnding(planName, days, DATE.format(end)));
    }

    private void processActive(SalonSubscription subscription, LocalDateTime now) {
        LocalDateTime end = subscription.getCurrentPeriodEnd();
        String planName = subscription.getPlan().getDisplayName();
        if (end == null) {
            end(subscription, now, planName);
            return;
        }
        if (!now.isBefore(end)) {
            LocalDateTime graceEnd = end.plusDays(SubscriptionPlan.GRACE_DAYS);
            if (!now.isBefore(graceEnd)) {
                // The job did not run during the whole grace time.
                end(subscription, now, planName);
                return;
            }
            subscription.setStatus(SubscriptionStatus.GRACE);
            subscription.setGraceEndsAt(graceEnd);
            notifyOnce(subscription, now, "GRACE:start:" + graceEnd.toLocalDate(),
                    emailTemplates.planPaymentDue(planName, DATE.format(graceEnd)));
            return;
        }
        remind(subscription, now, "PAID", end,
                days -> emailTemplates.planRenewing(planName, days, DATE.format(end)));
    }

    private void processGrace(SalonSubscription subscription, LocalDateTime now) {
        LocalDateTime end = subscription.getGraceEndsAt();
        String planName = subscription.getPlan().getDisplayName();
        if (end == null || !now.isBefore(end)) {
            end(subscription, now, planName);
            return;
        }
        if (daysLeft(now, end) <= LAST_REMINDER_DAYS && !hasOpenRequest(subscription)) {
            notifyOnce(subscription, now, "GRACE:1:" + end.toLocalDate(),
                    emailTemplates.planPaymentDue(planName, DATE.format(end)));
        }
    }

    /** The salon moves to Free. The plan on record stays, so the admin sees what ended. */
    private void end(SalonSubscription subscription, LocalDateTime now, String planName) {
        subscription.setStatus(SubscriptionStatus.ENDED);
        subscription.setGraceEndsAt(null);
        notifyOnce(subscription, now, "ENDED:" + now.toLocalDate(), emailTemplates.planEnded(planName));
    }

    private void remind(
            SalonSubscription subscription,
            LocalDateTime now,
            String stage,
            LocalDateTime end,
            java.util.function.LongFunction<EmailTemplate> template) {
        long days = daysLeft(now, end);
        int step = days <= LAST_REMINDER_DAYS ? LAST_REMINDER_DAYS
                : days <= FIRST_REMINDER_DAYS ? FIRST_REMINDER_DAYS
                : 0;
        if (step == 0 || hasOpenRequest(subscription)) {
            return;
        }
        notifyOnce(subscription, now, stage + ":" + step + ":" + end.toLocalDate(), template.apply(days));
    }

    private void notifyOnce(SalonSubscription subscription, LocalDateTime now, String key, EmailTemplate template) {
        subscription.setUpdatedAt(now);
        if (key.equals(subscription.getLastNoticeKey())) {
            subscriptionRepo.save(subscription);
            return;
        }
        subscription.setLastNoticeKey(key);
        subscriptionRepo.save(subscription);
        Salon salon = subscription.getSalon();
        User owner = salon == null ? null : salon.getOwner();
        if (owner != null && owner.getEmail() != null) {
            // Sent after the commit, so a failed save sends nothing.
            emailService.queue(owner.getEmail(), EmailNames.firstName(owner), template);
        }
    }

    private boolean hasOpenRequest(SalonSubscription subscription) {
        return paymentRepo
                .findFirstBySubscriptionIdAndStatus(subscription.getId(), SubscriptionPaymentStatus.PENDING)
                .isPresent();
    }

    /** Whole days left, rounded up, so "ends tomorrow" counts as 1. */
    private static long daysLeft(LocalDateTime now, LocalDateTime end) {
        return Math.max(0, (long) Math.ceil(Duration.between(now, end).toMinutes() / 1440.0));
    }
}
