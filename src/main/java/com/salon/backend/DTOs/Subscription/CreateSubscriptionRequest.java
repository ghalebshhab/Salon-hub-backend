package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.BillingCycle;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentMethod;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

/**
 * The owner asks for a paid plan. {@code reference} is the CliQ transfer
 * reference; it may be empty when the owner pays later or in cash.
 */
public record CreateSubscriptionRequest(
        SubscriptionPlan plan,
        BillingCycle billingCycle,
        SubscriptionPaymentMethod method,
        String reference
) {
}
