package com.salon.backend.Entities.subscriptions;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The salon plans: price, limits, card fee and features, in one place.
 * Prices are in fils (1 JOD = 1000 fils). A null limit means unlimited.
 * Customers never pay; only salons have a plan.
 */
public enum SubscriptionPlan {

    FREE("Free", 0, 0, 3, 15, 1, 7, 0, EnumSet.noneOf(PlanFeature.class)),

    PRO("Pro", 15_000, 150_000, 8, null, 3, 90, 300, EnumSet.of(
            PlanFeature.CARD_PAYMENTS,
            PlanFeature.CANCELLATION_FEES,
            PlanFeature.MISSED_VISIT_RULE,
            PlanFeature.EMAIL_REMINDERS,
            PlanFeature.ADVANCED_ANALYTICS,
            PlanFeature.PLAN_BADGE
    )),

    BUSINESS("Business", 25_000, 250_000, null, null, null, 366, 200, EnumSet.of(
            PlanFeature.CARD_PAYMENTS,
            PlanFeature.CANCELLATION_FEES,
            PlanFeature.MISSED_VISIT_RULE,
            PlanFeature.EMAIL_REMINDERS,
            PlanFeature.ADVANCED_ANALYTICS,
            PlanFeature.PLAN_BADGE,
            PlanFeature.CSV_EXPORT,
            PlanFeature.SEARCH_BOOST,
            PlanFeature.MANAGER_ROLE
    ));

    /** The plan a new salon tries for {@link #TRIAL_DAYS} days. */
    public static final SubscriptionPlan TRIAL_PLAN = PRO;
    public static final int TRIAL_DAYS = 30;
    /** Days a paid plan keeps working after the period ends, while the owner pays. */
    public static final int GRACE_DAYS = 7;

    private final String displayName;
    private final long monthlyPriceFils;
    private final long yearlyPriceFils;
    /** People in the team, the owner included. */
    private final Integer maxTeamMembers;
    private final Integer maxServices;
    private final Integer maxOpenHiringPosts;
    /** Longest dashboard period the owner can choose. */
    private final int analyticsMaxDays;
    /** SalonHub fee on card payments, in basis points (300 = 3%). */
    private final int cardFeeBps;
    private final Set<PlanFeature> features;

    SubscriptionPlan(
            String displayName,
            long monthlyPriceFils,
            long yearlyPriceFils,
            Integer maxTeamMembers,
            Integer maxServices,
            Integer maxOpenHiringPosts,
            int analyticsMaxDays,
            int cardFeeBps,
            Set<PlanFeature> features) {
        this.displayName = displayName;
        this.monthlyPriceFils = monthlyPriceFils;
        this.yearlyPriceFils = yearlyPriceFils;
        this.maxTeamMembers = maxTeamMembers;
        this.maxServices = maxServices;
        this.maxOpenHiringPosts = maxOpenHiringPosts;
        this.analyticsMaxDays = analyticsMaxDays;
        this.cardFeeBps = cardFeeBps;
        this.features = Collections.unmodifiableSet(features);
    }

    public boolean has(PlanFeature feature) {
        return features.contains(feature);
    }

    public boolean isPaid() {
        return this != FREE;
    }

    public long priceFils(BillingCycle cycle) {
        return cycle == BillingCycle.YEARLY ? yearlyPriceFils : monthlyPriceFils;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getMonthlyPriceFils() {
        return monthlyPriceFils;
    }

    public long getYearlyPriceFils() {
        return yearlyPriceFils;
    }

    public Integer getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public Integer getMaxServices() {
        return maxServices;
    }

    public Integer getMaxOpenHiringPosts() {
        return maxOpenHiringPosts;
    }

    public int getAnalyticsMaxDays() {
        return analyticsMaxDays;
    }

    public int getCardFeeBps() {
        return cardFeeBps;
    }

    public Set<PlanFeature> getFeatures() {
        return features;
    }
}
