package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

/** The admin moves a salon to another plan without a payment. */
public record ChangePlanRequest(SubscriptionPlan plan) {
}
