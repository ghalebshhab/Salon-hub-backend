package com.salon.backend.DTOs.Payment;

import com.salon.backend.Entities.payments.PaymentMethod;

import java.util.List;

/** What a customer can choose when booking this salon. */
public record PaymentOptionsResponse(
        Long salonId,
        List<PaymentMethod> methods,
        PaymentMethod defaultMethod,
        String currency,
        int freeCancelHours
) {
}
