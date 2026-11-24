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

    /**
     * The money can be recorded now. For CliQ the owner confirms the transfer
     * from the time the salon accepts the booking; for the other methods staff
     * record cash when the service starts or ends.
     */
    public static boolean isCollectable(Booking booking) {
        BookingPayment payment = booking.getPayment();
        if (payment != null && payment.getMethod() == PaymentMethod.CLIQ) {
            return isCliqConfirmable(booking);
        }
        return isCashCollectable(booking);
    }

    /** The owner can confirm a CliQ transfer: CliQ chosen, not paid, booking accepted, running or done. */
    public static boolean isCliqConfirmable(Booking booking) {
        BookingPayment payment = booking.getPayment();
        return payment != null
                && payment.getMethod() == PaymentMethod.CLIQ
                && (payment.getStatus() == PaymentStatus.UNPAID || payment.getStatus() == PaymentStatus.FAILED)
                && (booking.getStatus() == BookingStatus.ACCEPTED
                        || booking.getStatus() == BookingStatus.IN_PROGRESS
                        || booking.getStatus() == BookingStatus.COMPLETED);
    }

    /** The customer can switch between cash and CliQ: unpaid, and the service has not started. */
    public static boolean canChangeMethod(Booking booking) {
        BookingPayment payment = booking.getPayment();
        return payment != null
                && (payment.getMethod() == PaymentMethod.CASH || payment.getMethod() == PaymentMethod.CLIQ)
                && payment.getStatus() == PaymentStatus.UNPAID
                && (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.ACCEPTED);
    }

    /** Staff can record cash now: service started or done, nothing received, not paid online in advance. */
    public static boolean isCashCollectable(Booking booking) {
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
                isCollectable(booking),
                payment.getMethod() == PaymentMethod.CLIQ ? payment.getCliqAlias() : null,
                payment.getMethod() == PaymentMethod.CLIQ ? payment.getCliqName() : null
        );
    }
}
