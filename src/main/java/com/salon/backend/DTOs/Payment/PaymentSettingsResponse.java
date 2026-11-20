package com.salon.backend.DTOs.Payment;

/**
 * Owner view of the salon payment settings. {@code cardGatewayReady} is false
 * until a card provider is connected; card methods cannot be turned on before.
 * {@code commissionRateBps} is the platform rate (read only).
 */
public record PaymentSettingsResponse(
        Long salonId,
        boolean cashEnabled,
        boolean cardPrepaidEnabled,
        boolean cardPostpaidEnabled,
        int freeCancelHours,
        int lateCancelFeePercent,
        int noShowFeePercent,
        boolean cardGatewayReady,
        int commissionRateBps,
        /** Null when the no-show deposit rule is off. */
        Integer noShowLimit
) {
}
