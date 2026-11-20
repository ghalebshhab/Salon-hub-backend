package com.salon.backend.Services.Subscription;

import com.salon.backend.DTOs.Subscription.PlanResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.subscriptions.SalonSubscription;
import com.salon.backend.Entities.subscriptions.SubscriptionPayment;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;
import com.salon.backend.Repositories.Subscription.SalonSubscriptionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Salon plans: the price list, the plan a salon may use now, and the trial
 * that starts when an admin approves a salon.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SalonSubscriptionRepo subscriptionRepo;
    private final Clock clock;

    /** The public price list, cheapest first. */
    public List<PlanResponse> catalog() {
        return Arrays.stream(SubscriptionPlan.values()).map(SubscriptionService::toPlanResponse).toList();
    }

    /** The plan the salon may use now. Free when it has no subscription row. */
    @Transactional(readOnly = true)
    public SubscriptionPlan currentPlan(Salon salon) {
        if (salon == null || salon.getId() == null) {
            return SubscriptionPlan.FREE;
        }
        return subscriptionRepo.findBySalonId(salon.getId())
                .map(subscription -> subscription.planAt(now()))
                .orElse(SubscriptionPlan.FREE);
    }

    /** The plan in use for each salon id, in one query. Salons without a record are on Free. */
    @Transactional(readOnly = true)
    public java.util.Map<Long, SubscriptionPlan> currentPlans(java.util.Collection<Long> salonIds) {
        java.util.Map<Long, SubscriptionPlan> plans = new java.util.HashMap<>();
        if (salonIds == null || salonIds.isEmpty()) {
            return plans;
        }
        LocalDateTime now = now();
        for (SalonSubscription subscription : subscriptionRepo.findBySalonIdIn(salonIds)) {
            plans.put(subscription.getSalon().getId(), subscription.planAt(now));
        }
        return plans;
    }

    /** The badge a salon shows on public pages, or null on a plan without one. */
    public static String badgeOf(SubscriptionPlan plan) {
        return plan != null && plan.has(com.salon.backend.Entities.subscriptions.PlanFeature.PLAN_BADGE)
                ? plan.name()
                : null;
    }

    /** True when the plan places the salon higher in lists and search. */
    public static boolean boosted(SubscriptionPlan plan) {
        return plan != null && plan.has(com.salon.backend.Entities.subscriptions.PlanFeature.SEARCH_BOOST);
    }

    /**
     * Starts the free trial for a newly approved salon. Does nothing when the
     * salon already has a subscription, so approving twice never resets it.
     */
    @Transactional
    public void startTrial(Salon salon) {
        if (salon == null || salon.getId() == null || subscriptionRepo.existsBySalonId(salon.getId())) {
            return;
        }
        LocalDateTime now = now();
        SalonSubscription subscription = new SalonSubscription();
        subscription.setSalon(salon);
        subscription.setPlan(SubscriptionPlan.TRIAL_PLAN);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setTrialEndsAt(now.plusDays(SubscriptionPlan.TRIAL_DAYS));
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        subscriptionRepo.save(subscription);
    }


    public static PlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new PlanResponse(
                plan,
                plan.getDisplayName(),
                plan.getMonthlyPriceFils(),
                plan.getYearlyPriceFils(),
                SubscriptionPayment.CURRENCY,
                plan.getMaxTeamMembers(),
                plan.getMaxServices(),
                plan.getMaxOpenHiringPosts(),
                plan.getAnalyticsMaxDays(),
                plan.getCardFeeBps(),
                List.copyOf(plan.getFeatures())
        );
    }



    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
