package com.salon.backend.Entities.payments;

/** How the customer pays for a booking. */
public enum PaymentMethod {
    /** Cash at the salon after the service. */
    CASH,
    /** Card online, held at request and charged when the salon accepts. Needs a payment gateway. */
    CARD_PREPAID,
    /** Card online after the service, from a payment link. Needs a payment gateway. */
    CARD_POSTPAID,
    /**
     * CliQ instant transfer (Jordan) from the customer's bank app to the
     * salon's CliQ alias. No gateway: the salon owner confirms that the money
     * arrived.
     */
    CLIQ;

    public boolean needsGateway() {
        return this == CARD_PREPAID || this == CARD_POSTPAID;
    }
}
