package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.PaymentOptionsResponse;
import com.salon.backend.DTOs.Payment.PaymentSettingsRequest;
import com.salon.backend.DTOs.Payment.PaymentSettingsResponse;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.SalonPaymentSettings;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.payments.BookingPayment;

public interface PaymentSettingsService {

    /** Saved settings, or unsaved defaults (cash only). Never null. */
    SalonPaymentSettings effectiveSettings(Salon salon);

    /** Null when the customer may choose this method at this salon, else the reason. */
    String methodError(Salon salon, PaymentMethod method);

    PaymentMethod defaultMethod(Salon salon);

    /** Copies the salon's current CliQ alias and name onto the payment. */
    void copyCliqDetails(Salon salon, BookingPayment payment);

    ApiResponse<PaymentOptionsResponse> getOptions(Long salonId);

    ApiResponse<PaymentSettingsResponse> getSettings(Long salonId, Long ownerId);

    ApiResponse<PaymentSettingsResponse> updateSettings(Long salonId, PaymentSettingsRequest request, Long ownerId);
}
