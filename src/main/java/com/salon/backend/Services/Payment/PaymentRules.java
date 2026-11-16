package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.Payment.PaymentSummaryResponse;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;

/** Pure payment rules that both lists and actions use. */
public final class PaymentRules {

    private PaymentRules() {
    }

    /** Staff can record the money now: service started or done, nothing received, not paid online in advance. */
    public static boolean isCollectable(Booking booking) {
        BookingPayment payment = booking.getPayment();
        return payment != null
                && (booking.getStatus() == BookingStatus.IN_PROGRESS || booking.getStatus() == BookingStatus.COMPLETED)
                && (payment.getStatus() == PaymentStatus.UNPAID || payment.getStatus() == PaymentStatus.FAILED)
                && payment.getMethod() != PaymentMethod.CARD_PREPAID;
    }

    /** Null for bookings made before payments existed. */
    public static PaymentSummaryResponse summary(Booking booking) {
        BookingPayment payment = booking.getPayment();
        if (payment == null) {
            return null;
        }
        return new PaymentSummaryResponse(
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmountFils(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getReceiptNumber(),
                isCollectable(booking)
        );
    }
}
