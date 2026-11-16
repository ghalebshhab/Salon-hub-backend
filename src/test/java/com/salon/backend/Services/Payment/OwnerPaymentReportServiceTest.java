package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.OwnerPaymentSummaryResponse;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Payment.BookingPaymentRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Services.Salon.Booking.BookingResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerPaymentReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneId.of("Asia/Amman"));

    @Mock private BookingPaymentRepo paymentRepo;
    @Mock private SalonRepo salonRepo;

    private OwnerPaymentReportService service;

    @BeforeEach
    void setUp() {
        service = new OwnerPaymentReportService(
                paymentRepo, salonRepo, new BookingResponseMapper(), new PaymentPolicy(0), CLOCK);
    }

    @Test
    void theMonthSoFarIsSplitIntoCashAndCardWithUnpaidWork() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(activeSalon());
        when(paymentRepo.sumPaidByMethod(any(), any(), any(), any())).thenReturn(List.of(
                new Object[]{PaymentMethod.CASH, 3L, 45_000L, 0L, 0L},
                new Object[]{PaymentMethod.CARD_POSTPAID, 1L, 20_000L, 5_000L, 0L}
        ));
        when(paymentRepo.sumOutstanding(10L, BookingStatus.COMPLETED, PaymentStatus.UNPAID))
                .thenReturn(List.<Object[]>of(new Object[]{2L, 30_000L}));

        ApiResponse<OwnerPaymentSummaryResponse> response = service.getSummary(2L, null, null);

        assertTrue(response.isSuccess());
        OwnerPaymentSummaryResponse summary = response.getData();
        assertEquals(LocalDate.of(2026, 11, 1), summary.from());
        assertEquals(LocalDate.of(2026, 11, 16), summary.to());
        assertEquals(65_000L, summary.collectedFils());
        assertEquals(45_000L, summary.cashFils());
        assertEquals(20_000L, summary.cardFils());
        assertEquals(5_000L, summary.refundedFils());
        assertEquals(60_000L, summary.netFils());
        assertEquals(4L, summary.paidCount());
        assertEquals(30_000L, summary.outstandingFils());
        assertEquals(2L, summary.outstandingCount());
        assertEquals(0, summary.commissionRateBps());
        verify(paymentRepo).sumPaidByMethod(
                10L,
                EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 17, 0, 0));
    }

    @Test
    void aBadPeriodIsRefused() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(activeSalon());

        assertEquals("The start date cannot be after the end date", service.getSummary(
                2L, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)).getMessage());
        assertEquals("Choose a period of at most one year", service.getSummary(
                2L, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 1)).getMessage());
        verify(paymentRepo, never()).sumPaidByMethod(anyLong(), any(), any(), any());
    }

    @Test
    void onlyTheOwnerOfAnActiveSalonSeesTheMoney() {
        Salon blocked = activeSalon();
        blocked.setBlockedByAdmin(true);
        when(salonRepo.findByOwnerId(2L)).thenReturn(blocked);

        assertFalse(service.getSummary(2L, null, null).isSuccess());
        assertFalse(service.getOutstanding(2L, 0, 20).isSuccess());
    }

    @Test
    void theUnpaidListIsPaged() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(activeSalon());
        when(paymentRepo.findOutstandingBookings(any(), any(), any(), any())).thenReturn(List.of());

        assertTrue(service.getOutstanding(2L, 0, 20).isSuccess());
        assertFalse(service.getOutstanding(2L, 0, 500).isSuccess());
        assertFalse(service.getOutstanding(2L, -1, 20).isSuccess());
    }

    private Salon activeSalon() {
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setStatus(SalonStatus.Accepted);
        return salon;
    }
}
