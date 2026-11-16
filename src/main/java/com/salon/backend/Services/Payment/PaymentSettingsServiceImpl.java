package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.PaymentOptionsResponse;
import com.salon.backend.DTOs.Payment.PaymentSettingsRequest;
import com.salon.backend.DTOs.Payment.PaymentSettingsResponse;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.SalonPaymentSettings;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Repositories.Payment.SalonPaymentSettingsRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentSettingsServiceImpl implements PaymentSettingsService {

    static final int MAX_FREE_CANCEL_HOURS = 168;
    static final int MAX_PERCENT = 100;

    private final SalonPaymentSettingsRepo settingsRepo;
    private final SalonRepo salonRepo;
    private final PaymentGateway paymentGateway;
    private final PaymentPolicy paymentPolicy;
    private final Clock clock;

    @Override
    public SalonPaymentSettings effectiveSettings(Salon salon) {
        return settingsRepo.findBySalonId(salon.getId()).orElseGet(() -> {
            SalonPaymentSettings defaults = new SalonPaymentSettings();
            defaults.setSalon(salon);
            return defaults;
        });
    }

    @Override
    public String methodError(Salon salon, PaymentMethod method) {
        if (method == null) {
            return "Choose how you want to pay";
        }
        if (method.needsGateway() && !paymentGateway.isConfigured()) {
            return "Card payment is not available yet. Choose cash";
        }
        if (!effectiveSettings(salon).allows(method)) {
            return "This salon does not accept this payment method";
        }
        return null;
    }

    @Override
    public PaymentMethod defaultMethod(Salon salon) {
        List<PaymentMethod> methods = availableMethods(effectiveSettings(salon));
        return methods.isEmpty() ? null : methods.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PaymentOptionsResponse> getOptions(Long salonId) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        if (!isActive(salon)) {
            return ApiResponse.error("Active salon not found");
        }
        SalonPaymentSettings settings = effectiveSettings(salon);
        List<PaymentMethod> methods = availableMethods(settings);
        return ApiResponse.success("Payment options fetched successfully", new PaymentOptionsResponse(
                salon.getId(),
                methods,
                methods.isEmpty() ? null : methods.get(0),
                BookingPayment.CURRENCY,
                settings.getFreeCancelHours()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PaymentSettingsResponse> getSettings(Long salonId, Long ownerId) {
        Salon salon = findOwnedActiveSalon(salonId, ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        return ApiResponse.success("Payment settings fetched successfully", toResponse(effectiveSettings(salon)));
    }

    @Override
    @Transactional
    public ApiResponse<PaymentSettingsResponse> updateSettings(
            Long salonId,
            PaymentSettingsRequest request,
            Long ownerId) {
        Salon salon = findOwnedActiveSalon(salonId, ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        String error = validate(request);
        if (error != null) {
            return ApiResponse.error(error);
        }
        SalonPaymentSettings settings = effectiveSettings(salon);
        settings.setCashEnabled(request.cashEnabled());
        settings.setCardPrepaidEnabled(request.cardPrepaidEnabled());
        settings.setCardPostpaidEnabled(request.cardPostpaidEnabled());
        settings.setFreeCancelHours(request.freeCancelHours());
        settings.setLateCancelFeePercent(request.lateCancelFeePercent());
        settings.setNoShowFeePercent(request.noShowFeePercent());
        settings.setUpdatedAt(LocalDateTime.now(clock));
        settingsRepo.save(settings);
        return ApiResponse.success("Payment settings saved", toResponse(settings));
    }

    private String validate(PaymentSettingsRequest request) {
        if (request == null
                || request.cashEnabled() == null
                || request.cardPrepaidEnabled() == null
                || request.cardPostpaidEnabled() == null
                || request.freeCancelHours() == null
                || request.lateCancelFeePercent() == null
                || request.noShowFeePercent() == null) {
            return "Every payment setting is required";
        }
        boolean anyCard = request.cardPrepaidEnabled() || request.cardPostpaidEnabled();
        if (anyCard && !paymentGateway.isConfigured()) {
            return "Card payment needs a payment provider. It is not connected yet";
        }
        if (!request.cashEnabled() && !anyCard) {
            return "Keep at least one payment method on";
        }
        if (request.freeCancelHours() < 0 || request.freeCancelHours() > MAX_FREE_CANCEL_HOURS) {
            return "Free cancellation must be 0 to " + MAX_FREE_CANCEL_HOURS + " hours";
        }
        if (outOfPercent(request.lateCancelFeePercent()) || outOfPercent(request.noShowFeePercent())) {
            return "Fees must be 0 to " + MAX_PERCENT + " percent";
        }
        return null;
    }

    private boolean outOfPercent(int value) {
        return value < 0 || value > MAX_PERCENT;
    }

    private List<PaymentMethod> availableMethods(SalonPaymentSettings settings) {
        return Arrays.stream(PaymentMethod.values())
                .filter(settings::allows)
                .filter(method -> !method.needsGateway() || paymentGateway.isConfigured())
                .toList();
    }

    private PaymentSettingsResponse toResponse(SalonPaymentSettings settings) {
        return new PaymentSettingsResponse(
                settings.getSalon().getId(),
                settings.isCashEnabled(),
                settings.isCardPrepaidEnabled(),
                settings.isCardPostpaidEnabled(),
                settings.getFreeCancelHours(),
                settings.getLateCancelFeePercent(),
                settings.getNoShowFeePercent(),
                paymentGateway.isConfigured(),
                paymentPolicy.commissionRateBps()
        );
    }

    private Salon findOwnedActiveSalon(Long salonId, Long ownerId) {
        if (salonId == null || ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return isActive(salon) && salon.getId().equals(salonId) ? salon : null;
    }

    private boolean isActive(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }
}
