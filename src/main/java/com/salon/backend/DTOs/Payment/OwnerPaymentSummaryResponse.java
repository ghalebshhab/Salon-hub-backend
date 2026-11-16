package com.salon.backend.DTOs.Payment;

import java.time.LocalDate;

/**
 * Money of the owner's salon in a date range (paid time), plus everything that
 * is still outstanding. All amounts are fils.
 */
public record OwnerPaymentSummaryResponse(
        LocalDate from,
        LocalDate to,
        long collectedFils,
        long cashFils,
        long cardFils,
        long refundedFils,
        long commissionFils,
        long netFils,
        long paidCount,
        long outstandingFils,
        long outstandingCount,
        int commissionRateBps,
        String currency
) {
}
