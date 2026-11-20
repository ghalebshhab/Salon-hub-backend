package com.salon.backend.Services.Subscription;

import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The checks that each plan limit needs, in one place.
 *
 * <p>Every check returns {@code null} (or {@code false} for {@link #locked})
 * when the action is allowed, so callers read "no error means go on".
 */
@Component
@RequiredArgsConstructor
public class PlanLimits {

    private final SubscriptionService subscriptionService;
    private final SalonServiceRepo salonServiceRepo;
    private final HiringPostRepo hiringPostRepo;

    public SubscriptionPlan plan(Salon salon) {
        return subscriptionService.currentPlan(salon);
    }

    /** True when the salon's plan does not include the feature. */
    public boolean locked(Salon salon, PlanFeature feature) {
        return !plan(salon).has(feature);
    }

    /** Null when the plan includes the feature, else a message for the owner. */
    public String featureError(Salon salon, PlanFeature feature, String what) {
        SubscriptionPlan plan = plan(salon);
        if (plan.has(feature)) {
            return null;
        }
        return what + " is not part of the " + plan.getDisplayName()
                + " plan. Upgrade your plan to use it.";
    }

    /**
     * Null when one more person can join the team. The owner counts as a
     * team member, the same as {@code currentEmployeesNumber}.
     */
    public String teamFullError(Salon salon) {
        SubscriptionPlan plan = plan(salon);
        Integer max = plan.getMaxTeamMembers();
        if (max == null || salon == null) {
            return null;
        }
        long current = salon.getCurrentEmployeesNumber() == null ? 1 : salon.getCurrentEmployeesNumber();
        if (current < max) {
            return null;
        }
        return "The " + plan.getDisplayName() + " plan of this salon allows up to " + max
                + " team members, including the owner. The owner must upgrade the plan to add more.";
    }

    /** Null when the salon can have one more active service. */
    public String serviceLimitError(Salon salon) {
        SubscriptionPlan plan = plan(salon);
        Integer max = plan.getMaxServices();
        if (max == null || salon == null || salon.getId() == null) {
            return null;
        }
        if (salonServiceRepo.countBySalonIdAndActiveTrue(salon.getId()) < max) {
            return null;
        }
        return "The " + plan.getDisplayName() + " plan allows up to " + max
                + " active services. Turn off a service or upgrade your plan to add more.";
    }

    /** Null when the salon can have one more open hiring post. */
    public String hiringPostLimitError(Salon salon) {
        SubscriptionPlan plan = plan(salon);
        Integer max = plan.getMaxOpenHiringPosts();
        if (max == null || salon == null || salon.getId() == null) {
            return null;
        }
        if (hiringPostRepo.countBySalonIdAndStatus(salon.getId(), HiringPostStatus.Open) < max) {
            return null;
        }
        String posts = max == 1 ? "1 open hiring post" : max + " open hiring posts";
        return "The " + plan.getDisplayName() + " plan allows " + posts
                + ". Close a post or upgrade your plan to open more.";
    }

    /** The platform fee for a new payment, in basis points. Cash has no fee. */
    public int cardFeeBps(Salon salon, PaymentMethod method) {
        if (method == null || !method.needsGateway()) {
            return 0;
        }
        return plan(salon).getCardFeeBps();
    }

    /** The longest date range the dashboard may show, in days. */
    public int analyticsMaxDays(Salon salon) {
        return plan(salon).getAnalyticsMaxDays();
    }
}
