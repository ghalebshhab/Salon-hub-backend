package com.salon.backend.Entities.subscriptions;

/** How often the salon pays. A year costs 10 months (2 months free). */
public enum BillingCycle {
    MONTHLY,
    YEARLY;

    public int months() {
        return this == YEARLY ? 12 : 1;
    }
}
