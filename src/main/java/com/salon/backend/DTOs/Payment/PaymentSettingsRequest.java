package com.salon.backend.DTOs.Payment;

public record PaymentSettingsRequest(
        Boolean cashEnabled,
        Boolean cardPrepaidEnabled,
        Boolean cardPostpaidEnabled,
        Integer freeCancelHours,
        Integer lateCancelFeePercent,
        Integer noShowFeePercent
) {
}
