package com.salon.backend.Entities.subscriptions;

/** Plan features that are on or off. Number limits live on {@link SubscriptionPlan}. */
public enum PlanFeature {
    /** Card after the service and card in advance (when a provider is connected). */
    CARD_PAYMENTS,
    /** Late-cancellation and no-show fees on bookings paid by card in advance. */
    CANCELLATION_FEES,
    /** Customers with many no-shows must pay in advance. */
    MISSED_VISIT_RULE,
    /** Email reminders, the 2-hour reminder and the customer's "Yes, I'm coming". */
    EMAIL_REMINDERS,
    /** All dashboard panels: busy hours, payments, returning customers, team and customers. */
    ADVANCED_ANALYTICS,
    /** Download dashboard data as CSV. */
    CSV_EXPORT,
    /** Pro or Business badge on the public salon page. */
    PLAN_BADGE,
    /** Higher place in search results. */
    SEARCH_BOOST,
    /** A staff member who manages bookings for the owner. */
    MANAGER_ROLE
}
