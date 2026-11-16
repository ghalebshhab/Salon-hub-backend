package com.salon.backend.Services.Payment;

/**
 * Seam for an online card provider. Card methods stay closed until an
 * implementation reports that it is configured. Step B adds the payment link
 * and webhook; step C adds hold, capture, void and refund.
 */
public interface PaymentGateway {

    boolean isConfigured();

    /** Short provider name for logs and the ledger, for example "none". */
    String providerName();
}
