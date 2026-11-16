package com.salon.backend.DTOs.Payment;

import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Payment part of a booking in lists. {@code collectable} means staff can
 * record the money now (service started or finished, not paid, not prepaid).
 */
public record PaymentSummaryResponse(
        PaymentMethod method,
        PaymentStatus status,
        long amountFils,
        String currency,
        LocalDateTime paidAt,
        String receiptNumber,
        boolean collectable
) {
}
