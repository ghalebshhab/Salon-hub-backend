package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

/** One owner request in the admin queue, with the salon it belongs to. */
public record AdminPlanRequestResponse(
        Long salonId,
        String salonName,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        /** The plan the salon uses now. */
        SubscriptionPlan currentPlan,
        SubscriptionRequestResponse request
) {
}
