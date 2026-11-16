package com.salon.backend.Entities.payments;

/** How the customer pays for a booking. */
public enum PaymentMethod {
    /** Cash at the salon after the service. */
    CASH,
    /** Card online, held at request and charged when the salon accepts. Needs a payment gateway. */
    CARD_PREPAID,
    /** Card online after the service, from a payment link. Needs a payment gateway. */
    CARD_POSTPAID;

    public boolean needsGateway() {
        return this != CASH;
    }
}
