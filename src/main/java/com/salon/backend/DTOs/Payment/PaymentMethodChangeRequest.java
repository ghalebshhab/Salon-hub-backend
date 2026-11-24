package com.salon.backend.DTOs.Payment;

import com.salon.backend.Entities.payments.PaymentMethod;

/** The customer switches an unpaid booking between CASH and CLIQ. */
public record PaymentMethodChangeRequest(PaymentMethod method) {
}
