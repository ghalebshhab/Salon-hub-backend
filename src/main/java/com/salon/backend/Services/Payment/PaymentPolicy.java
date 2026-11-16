package com.salon.backend.Services.Payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money rules in one place. The Jordanian dinar has three decimals, so all
 * amounts are whole fils (1 JD = 1000 fils).
 */
@Component
public class PaymentPolicy {

    public static final int FILS_PER_DINAR = 1000;
    public static final int MAX_BPS = 10_000;

    private final int commissionRateBps;

    public PaymentPolicy(@Value("${salon.payments.commission-rate-bps:0}") int commissionRateBps) {
        if (commissionRateBps < 0 || commissionRateBps > MAX_BPS) {
            throw new IllegalArgumentException("salon.payments.commission-rate-bps must be 0 to " + MAX_BPS);
        }
        this.commissionRateBps = commissionRateBps;
    }

    /** Platform commission for new payments, in basis points (100 = 1%). Zero for now. */
    public int commissionRateBps() {
        return commissionRateBps;
    }

    public static long toFils(BigDecimal dinars) {
        if (dinars == null) {
            return 0;
        }
        return dinars.movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static long commissionFils(long amountFils, int rateBps) {
        return BigDecimal.valueOf(amountFils)
                .multiply(BigDecimal.valueOf(rateBps))
                .divide(BigDecimal.valueOf(MAX_BPS), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Readable, unique receipt number, for example SH-1-000042. */
    public static String receiptNumber(Long salonId, Long paymentId) {
        return String.format("SH-%d-%06d", salonId, paymentId);
    }
}
