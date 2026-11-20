package com.salon.backend.DTOs.Subscription;

import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;

import java.util.List;

/** One plan of the public price list. Prices in fils; a null limit means unlimited. */
public record PlanResponse(
        SubscriptionPlan code,
        String name,
        long monthlyPriceFils,
        long yearlyPriceFils,
        String currency,
        Integer maxTeamMembers,
        Integer maxServices,
        Integer maxOpenHiringPosts,
        int analyticsMaxDays,
        int cardFeeBps,
        List<PlanFeature> features
) {
}
