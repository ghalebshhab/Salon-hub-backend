package com.salon.backend.DTOs.Payment;

import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Receipt and payment detail of one booking. The ledger, commission and net
 * amount are sent only to the salon owner; other viewers get null or empty.
 */
public record BookingPaymentResponse(
        Long bookingId,
        String viewerRole,
        String receiptNumber,
        PaymentMethod method,
        PaymentStatus status,
        long amountFils,
        long refundedFils,
        String currency,
        LocalDateTime paidAt,
        String confirmedByName,
        String salonName,
        String customerName,
        LocalDate bookingDate,
        LocalTime bookingTime,
        List<ReceiptLineResponse> lines,
        boolean collectable,
        Long commissionFils,
        Long netFils,
        List<PaymentTransactionResponse> transactions
) {

    public record ReceiptLineResponse(String name, long amountFils, int durationMinutes) {
    }

    public record PaymentTransactionResponse(
            String type,
            long amountFils,
            String actorName,
            String note,
            LocalDateTime createdAt
    ) {
    }
}
