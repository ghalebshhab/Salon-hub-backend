package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.BillingCycle;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentMethod;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentStatus;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

import java.time.LocalDateTime;

/** One plan payment request of the owner and its review result. */
public record SubscriptionRequestResponse(
        Long id,
        SubscriptionPlan plan,
        BillingCycle billingCycle,
        long amountFils,
        String currency,
        SubscriptionPaymentMethod method,
        String reference,
        SubscriptionPaymentStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime periodEnd
) {
}
