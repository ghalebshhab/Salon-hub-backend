package com.salon.backend.Entities.subscriptions;

/** An owner's upgrade or renewal payment, checked by an admin. */
public enum SubscriptionPaymentStatus {
    /** The owner says they paid; an admin has not checked it yet. */
    PENDING,
    /** An admin saw the money; the plan period is added. */
    CONFIRMED,
    /** An admin did not find the money. */
    REJECTED,
    /** The owner withdrew the request. */
    CANCELLED
}
