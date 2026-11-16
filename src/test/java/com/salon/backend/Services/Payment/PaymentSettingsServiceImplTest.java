package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.PaymentOptionsResponse;
import com.salon.backend.DTOs.Payment.PaymentSettingsRequest;
import com.salon.backend.DTOs.Payment.PaymentSettingsResponse;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.SalonPaymentSettings;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Repositories.Payment.SalonPaymentSettingsRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSettingsServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T08:00:00Z"), ZoneId.of("Asia/Amman"));

    @Mock private SalonPaymentSettingsRepo settingsRepo;
    @Mock private SalonRepo salonRepo;

    private final UnconfiguredPaymentGateway noGateway = new UnconfiguredPaymentGateway();
    private PaymentSettingsServiceImpl service;
    private Salon salon;

    @BeforeEach
    void setUp() {
        service = new PaymentSettingsServiceImpl(settingsRepo, salonRepo, noGateway, new PaymentPolicy(0), CLOCK);
        salon = new Salon();
        salon.setId(10L);
        salon.setStatus(SalonStatus.Accepted);
        lenient().when(settingsRepo.findBySalonId(10L)).thenReturn(Optional.empty());
    }

    @Test
    void aSalonWithoutSettingsOffersCashOnlyWithTwentyFourHourFreeCancel() {
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));

        ApiResponse<PaymentOptionsResponse> response = service.getOptions(10L);

        assertTrue(response.isSuccess());
        assertEquals(List.of(PaymentMethod.CASH), response.getData().methods());
        assertEquals(PaymentMethod.CASH, response.getData().defaultMethod());
        assertEquals("JOD", response.getData().currency());
        assertEquals(24, response.getData().freeCancelHours());
    }

    @Test
    void cardIsRefusedWhileNoProviderIsConnected() {
        assertEquals("Card payment is not available yet. Choose cash",
                service.methodError(salon, PaymentMethod.CARD_POSTPAID));
        assertNull(service.methodError(salon, PaymentMethod.CASH));
        assertEquals("Choose how you want to pay", service.methodError(salon, null));
    }

    @Test
    void aMethodTheSalonTurnedOffIsRefused() {
        SalonPaymentSettings settings = new SalonPaymentSettings();
        settings.setSalon(salon);
        settings.setCashEnabled(false);
        when(settingsRepo.findBySalonId(10L)).thenReturn(Optional.of(settings));

        assertEquals("This salon does not accept this payment method",
                service.methodError(salon, PaymentMethod.CASH));
    }

    @Test
    void theOwnerSavesFeesAndFreeCancelHours() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);

        ApiResponse<PaymentSettingsResponse> response = service.updateSettings(
                10L, new PaymentSettingsRequest(true, false, false, 12, 50, 100), 2L);

        assertTrue(response.isSuccess());
        assertEquals(12, response.getData().freeCancelHours());
        assertEquals(50, response.getData().lateCancelFeePercent());
        assertEquals(100, response.getData().noShowFeePercent());
        assertFalse(response.getData().cardGatewayReady());
        assertEquals(0, response.getData().commissionRateBps());
        verify(settingsRepo).save(any(SalonPaymentSettings.class));
    }

    @Test
    void invalidSettingsAreRefusedAndNothingIsSaved() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);

        assertEquals("Card payment needs a payment provider. It is not connected yet",
                service.updateSettings(10L, new PaymentSettingsRequest(true, true, false, 24, 0, 0), 2L).getMessage());
        assertEquals("Keep at least one payment method on",
                service.updateSettings(10L, new PaymentSettingsRequest(false, false, false, 24, 0, 0), 2L).getMessage());
        assertEquals("Free cancellation must be 0 to 168 hours",
                service.updateSettings(10L, new PaymentSettingsRequest(true, false, false, 200, 0, 0), 2L).getMessage());
        assertEquals("Fees must be 0 to 100 percent",
                service.updateSettings(10L, new PaymentSettingsRequest(true, false, false, 24, 101, 0), 2L).getMessage());
        assertEquals("Every payment setting is required",
                service.updateSettings(10L, new PaymentSettingsRequest(true, null, false, 24, 0, 0), 2L).getMessage());
        verify(settingsRepo, never()).save(any());
    }

    @Test
    void anotherOwnerCannotReadOrChangeTheSettings() {
        Salon other = new Salon();
        other.setId(99L);
        other.setStatus(SalonStatus.Accepted);
        when(salonRepo.findByOwnerId(5L)).thenReturn(other);

        assertFalse(service.getSettings(10L, 5L).isSuccess());
        assertFalse(service.updateSettings(10L, new PaymentSettingsRequest(true, false, false, 24, 0, 0), 5L)
                .isSuccess());
    }
}
