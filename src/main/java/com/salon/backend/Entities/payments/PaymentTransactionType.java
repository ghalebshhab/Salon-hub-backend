package com.salon.backend.Entities.payments;

/** One money movement or decision in the payment ledger. */
public enum PaymentTransactionType {
    CASH_RECEIVED,
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND,
    METHOD_CHANGED
}
