package com.salon.backend.DTOs.Payment;

/**
 * Owner payment settings. {@code noShowLimit} is optional: null turns the
 * no-show deposit rule off, 1 to 10 turns it on.
 */
public record PaymentSettingsRequest(
        Boolean cashEnabled,
        Boolean cardPrepaidEnabled,
        Boolean cardPostpaidEnabled,
        Integer freeCancelHours,
        Integer lateCancelFeePercent,
        Integer noShowFeePercent,
        Integer noShowLimit
) {

    /** Settings without the no-show deposit rule. */
    public PaymentSettingsRequest(
            Boolean cashEnabled,
            Boolean cardPrepaidEnabled,
            Boolean cardPostpaidEnabled,
            Integer freeCancelHours,
            Integer lateCancelFeePercent,
            Integer noShowFeePercent) {
        this(cashEnabled, cardPrepaidEnabled, cardPostpaidEnabled,
                freeCancelHours, lateCancelFeePercent, noShowFeePercent, null);
    }
}
