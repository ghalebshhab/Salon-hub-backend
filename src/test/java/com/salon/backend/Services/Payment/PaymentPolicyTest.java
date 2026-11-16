package com.salon.backend.Services.Payment;

import com.salon.backend.Entities.payments.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPolicyTest {

    @Test
    void dinarsBecomeWholeFilsWithThreeDecimals() {
        assertEquals(19_500L, PaymentPolicy.toFils(new BigDecimal("19.50")));
        assertEquals(7_125L, PaymentPolicy.toFils(new BigDecimal("7.125")));
        assertEquals(1L, PaymentPolicy.toFils(new BigDecimal("0.0005")), "Half a fils rounds up");
        assertEquals(0L, PaymentPolicy.toFils(null));
    }

    @Test
    void commissionIsZeroByDefaultAndRoundsHalfUpWhenSet() {
        assertEquals(0L, PaymentPolicy.commissionFils(19_500L, new PaymentPolicy(0).commissionRateBps()));
        assertEquals(975L, PaymentPolicy.commissionFils(19_500L, 500), "5% of 19.500 JOD");
        assertEquals(1L, PaymentPolicy.commissionFils(5L, 1_000), "0.5 fils rounds up");
    }

    @Test
    void aRateOutsideZeroToOneHundredPercentStopsTheStart() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentPolicy(-1));
        assertThrows(IllegalArgumentException.class, () -> new PaymentPolicy(10_001));
    }

    @Test
    void receiptNumbersAreReadableAndPadded() {
        assertEquals("SH-3-000042", PaymentPolicy.receiptNumber(3L, 42L));
    }

    @Test
    void statusMovesFollowTheAllowedMap() {
        assertTrue(PaymentStatus.UNPAID.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.UNPAID.canTransitionTo(PaymentStatus.VOIDED));
        assertTrue(PaymentStatus.PAID.canTransitionTo(PaymentStatus.REFUNDED));
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.VOIDED), "Received money is refunded, not voided");
        assertFalse(PaymentStatus.VOIDED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.PARTIALLY_REFUNDED.isSettled());
        assertFalse(PaymentStatus.UNPAID.isSettled());
    }
}
