package com.salon.backend.Entities.subscriptions;

/** How a salon pays SalonHub for its plan. */
public enum SubscriptionPaymentMethod {
    /** Instant bank transfer in Jordan; the owner gives the transfer reference. */
    CLIQ,
    /** Cash handed to the SalonHub team. */
    CASH,
    /** Free months given by an admin (launch offer, support, partners). */
    COMPLIMENTARY
}
