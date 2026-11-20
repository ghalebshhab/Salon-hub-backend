package com.salon.backend.Services.Payment;

import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.payments.PaymentTransaction;
import com.salon.backend.Entities.payments.PaymentTransactionType;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Services.Subscription.PlanLimits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Opens and closes the payment together with the booking. The callers own the
 * transaction and the booking lock; this class only changes the payment.
 */
@Component
public class BookingPaymentLifecycle {

    private final PaymentPolicy paymentPolicy;
    private final Clock clock;
    /** Null only in tests; then the policy's single rate applies. */
    private final PlanLimits planLimits;

    @Autowired
    public BookingPaymentLifecycle(PaymentPolicy paymentPolicy, Clock clock, PlanLimits planLimits) {
        this.paymentPolicy = paymentPolicy;
        this.clock = clock;
        this.planLimits = planLimits;
    }

    public BookingPaymentLifecycle(PaymentPolicy paymentPolicy, Clock clock) {
        this(paymentPolicy, clock, null);
    }

    /** New unpaid payment for a new booking; the booking total must be set first. */
    public BookingPayment open(Booking booking, PaymentMethod method) {
        LocalDateTime now = LocalDateTime.now(clock);
        long amount = PaymentPolicy.toFils(booking.getTotalPrice());
        // The card fee depends on the salon's plan; cash has no fee.
        int rate = planLimits == null
                ? paymentPolicy.commissionRateBps()
                : planLimits.cardFeeBps(booking.getSalon(), method);

        BookingPayment payment = new BookingPayment();
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAmountFils(amount);
        payment.setCommissionRateBps(rate);
        payment.setCommissionFils(PaymentPolicy.commissionFils(amount, rate));
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        booking.setPayment(payment);
        return payment;
    }

    /**
     * Closes a payment that never received money, when the booking ends without
     * a service (rejected, cancelled, expired, no-show). Safe to call twice.
     */
    public void closeWithoutPayment(Booking booking, String reason, User actor) {
        BookingPayment payment = booking.getPayment();
        if (payment == null || !payment.getStatus().canTransitionTo(PaymentStatus.VOIDED)) {
            return;
        }
        // Step C: an AUTHORIZED card hold is released at the gateway here.
        LocalDateTime now = LocalDateTime.now(clock);
        payment.moveTo(PaymentStatus.VOIDED);
        payment.setUpdatedAt(now);
        payment.addTransaction(transaction(
                PaymentTransactionType.VOID,
                0,
                "booking:" + booking.getId() + ":void",
                actor,
                reason,
                now
        ));
    }

    static PaymentTransaction transaction(
            PaymentTransactionType type,
            long amountFils,
            String idempotencyKey,
            User actor,
            String note,
            LocalDateTime at) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setType(type);
        transaction.setAmountFils(amountFils);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setActor(actor);
        transaction.setNote(note);
        transaction.setCreatedAt(at);
        return transaction;
    }
}
