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
        Integer noShowLimit,
        /** Null means off. */
        Boolean cliqEnabled,
        /** The salon's CliQ alias or mobile number. Required when CliQ is on. */
        String cliqAlias,
        /** Optional account holder name that customers see in their bank app. */
        String cliqName
) {
    /** Settings without CliQ. */
    public PaymentSettingsRequest(
            Boolean cashEnabled,
            Boolean cardPrepaidEnabled,
            Boolean cardPostpaidEnabled,
            Integer freeCancelHours,
            Integer lateCancelFeePercent,
            Integer noShowFeePercent,
            Integer noShowLimit) {
        this(cashEnabled, cardPrepaidEnabled, cardPostpaidEnabled,
                freeCancelHours, lateCancelFeePercent, noShowFeePercent, noShowLimit, null, null, null);
    }

    /** Settings without the no-show deposit rule. */
    public PaymentSettingsRequest(
            Boolean cashEnabled,
            Boolean cardPrepaidEnabled,
            Boolean cardPostpaidEnabled,
            Integer freeCancelHours,
            Integer lateCancelFeePercent,
            Integer noShowFeePercent) {
        this(cashEnabled, cardPrepaidEnabled, cardPostpaidEnabled,
                freeCancelHours, lateCancelFeePercent, noShowFeePercent, null, null, null, null);
    }
}
