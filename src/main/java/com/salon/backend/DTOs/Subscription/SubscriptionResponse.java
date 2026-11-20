package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.BillingCycle;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The owner's view of their salon plan. {@code plan} is the plan in use now;
 * {@code subscribedPlan} is the plan on record, which can differ after a trial
 * or period ended. {@code limits} are the limits of {@code plan}, and
 * {@code usage} is what the salon uses now.
 */
public record SubscriptionResponse(
        Long salonId,
        SubscriptionPlan plan,
        SubscriptionPlan subscribedPlan,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        LocalDateTime trialEndsAt,
        LocalDateTime currentPeriodEnd,
        LocalDateTime graceEndsAt,
        /** When the current paid or trial time ends; null on Free. */
        LocalDateTime accessEndsAt,
        /** Whole days left until {@code accessEndsAt}; null on Free. */
        Long daysLeft,
        PlanResponse limits,
        PlanUsage usage,
        /** The request waiting for an admin, or null. */
        SubscriptionRequestResponse openRequest,
        /** The latest requests, newest first. */
        List<SubscriptionRequestResponse> recentRequests,
        PayToResponse payTo
) {
}
