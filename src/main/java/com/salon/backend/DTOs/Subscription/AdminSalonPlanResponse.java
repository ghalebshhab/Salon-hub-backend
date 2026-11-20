package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.BillingCycle;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;

import java.time.LocalDateTime;

/** The plan state of one approved salon, for the admin list. */
public record AdminSalonPlanResponse(
        Long salonId,
        String salonName,
        String ownerName,
        SubscriptionPlan plan,
        SubscriptionPlan subscribedPlan,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        LocalDateTime accessEndsAt,
        Long daysLeft,
        boolean openRequest
) {
}
