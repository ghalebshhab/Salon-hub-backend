package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

/** Free months the admin gives a salon, for example a launch offer. */
public record GrantPlanRequest(SubscriptionPlan plan, Integer months, String note) {
}
