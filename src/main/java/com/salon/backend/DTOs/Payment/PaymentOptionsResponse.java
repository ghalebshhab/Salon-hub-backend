package com.salon.backend.DTOs.Payment;

import com.salon.backend.Entities.payments.PaymentMethod;

import java.util.List;

/**
 * What a customer can choose when booking this salon. {@code restricted} is
 * true when the salon's no-show rule limits this customer; the message says why.
 */
public record PaymentOptionsResponse(
        Long salonId,
        List<PaymentMethod> methods,
        PaymentMethod defaultMethod,
        String currency,
        int freeCancelHours,
        boolean restricted,
        String restrictionMessage,
        /** Where to send a CliQ transfer; null unless CLIQ is in {@code methods}. */
        String cliqAlias,
        String cliqName
) {
    public PaymentOptionsResponse(
            Long salonId,
            List<PaymentMethod> methods,
            PaymentMethod defaultMethod,
            String currency,
            int freeCancelHours,
            boolean restricted,
            String restrictionMessage) {
        this(salonId, methods, defaultMethod, currency, freeCancelHours, restricted, restrictionMessage, null, null);
    }


    public PaymentOptionsResponse(
            Long salonId,
            List<PaymentMethod> methods,
            PaymentMethod defaultMethod,
            String currency,
            int freeCancelHours) {
        this(salonId, methods, defaultMethod, currency, freeCancelHours, false, null, null, null);
    }
}
