package com.salon.backend.Entities.payments;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Money state of one booking, separate from the booking status. The allowed
 * moves are listed here so every service checks the same rules.
 */
public enum PaymentStatus {
    UNPAID,
    /** Card amount held, not yet charged. */
    AUTHORIZED,
    PAID,
    PARTIALLY_REFUNDED,
    REFUNDED,
    /** Closed without money: booking rejected, cancelled, expired or missed before payment. */
    VOIDED,
    /** Last card attempt failed; the customer can try again. */
    FAILED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            UNPAID, EnumSet.of(AUTHORIZED, PAID, VOIDED, FAILED),
            AUTHORIZED, EnumSet.of(PAID, VOIDED, FAILED),
            PAID, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED),
            PARTIALLY_REFUNDED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED),
            REFUNDED, EnumSet.noneOf(PaymentStatus.class),
            VOIDED, EnumSet.noneOf(PaymentStatus.class),
            FAILED, EnumSet.of(AUTHORIZED, PAID, VOIDED, FAILED)
    );

    public boolean canTransitionTo(PaymentStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** Money was received and not fully returned. */
    public boolean isSettled() {
        return this == PAID || this == PARTIALLY_REFUNDED;
    }
}
