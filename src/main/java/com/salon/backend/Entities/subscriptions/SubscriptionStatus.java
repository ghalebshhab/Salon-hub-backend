package com.salon.backend.Entities.subscriptions;

/**
 * Where a salon's subscription is in its life. The plan in use also depends on
 * the dates: a trial or period that has passed counts as Free even before the
 * daily job updates this status.
 */
public enum SubscriptionStatus {
    /** Free trial of the trial plan until {@code trialEndsAt}. */
    TRIAL,
    /** Paid until {@code currentPeriodEnd}. */
    ACTIVE,
    /** The period ended; the plan keeps working until {@code graceEndsAt}. */
    GRACE,
    /** Trial or paid time is over; the salon uses Free. */
    ENDED
}
