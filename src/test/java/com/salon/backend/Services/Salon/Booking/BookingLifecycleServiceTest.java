package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.BookingRescheduleProposalRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.payments.PaymentTransactionType;
import com.salon.backend.Services.Payment.BookingPaymentLifecycle;
import com.salon.backend.Services.Payment.PaymentPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {

    /** 10:00 local time on 2026-10-17. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T07:00:00Z"), ZoneId.of("Asia/Amman"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime NOW = LocalDateTime.of(TODAY, LocalTime.of(10, 0));

    @Mock private BookingRepo bookingRepo;
    @Mock private BookingRescheduleProposalRepo rescheduleProposalRepo;
    @Mock private BookingNotificationService notificationService;

    private BookingLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new BookingLifecycleService(bookingRepo, rescheduleProposalRepo, notificationService, CLOCK,
                new BookingPaymentLifecycle(new PaymentPolicy(0), CLOCK));
    }

    @Test
    void anExpiredRequestClosesItsUnpaidPayment() {
        Booking booking = booking(BookingStatus.PENDING, TODAY.plusDays(3), NOW.minusHours(25));
        booking.setTotalPrice(new BigDecimal("12.500"));
        new BookingPaymentLifecycle(new PaymentPolicy(0), CLOCK).open(booking, PaymentMethod.CASH);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertTrue(service.expireIfOverdue(30L));

        assertEquals(PaymentStatus.VOIDED, booking.getPayment().getStatus());
        assertEquals(PaymentTransactionType.VOID, booking.getPayment().getTransactions().get(0).getType());
        assertNull(booking.getPayment().getTransactions().get(0).getActor());
    }

    @Test
    void findsOverdueRequestsWithTheResponseWindowAndTheCurrentTime() {
        when(bookingRepo.findOverduePendingIds(any(), any(), any(), any())).thenReturn(List.of(5L));

        assertEquals(List.of(5L), service.findOverduePendingIds());
        verify(bookingRepo).findOverduePendingIds(
                BookingStatus.PENDING, NOW.minusHours(24), TODAY, LocalTime.of(10, 0));
    }

    @Test
    void anUnansweredRequestExpiresAndBothSidesAreTold() {
        Booking booking = booking(BookingStatus.PENDING, TODAY.plusDays(3), NOW.minusHours(25));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertTrue(service.expireIfOverdue(30L));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(NOW, booking.getDecidedAt());
        verify(rescheduleProposalRepo).cancelPendingForBooking(
                30L, BookingRescheduleStatus.PENDING, BookingRescheduleStatus.CANCELLED, NOW);
        verify(bookingRepo).save(booking);
        verify(notificationService).bookingExpired(booking);
    }

    @Test
    void aRequestStillInsideItsWindowDoesNotExpire() {
        Booking booking = booking(BookingStatus.PENDING, TODAY.plusDays(3), NOW.minusHours(2));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertFalse(service.expireIfOverdue(30L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(notificationService, never()).bookingExpired(any());
    }

    @Test
    void aRequestAnsweredMeanwhileIsNotChanged() {
        Booking booking = booking(BookingStatus.ACCEPTED, TODAY.plusDays(3), NOW.minusHours(30));
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertFalse(service.expireIfOverdue(30L));
        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
        verify(rescheduleProposalRepo, never()).cancelPendingForBooking(anyLong(), any(), any(), any());
    }

    @Test
    void remindersAreLookedForTodayAndTomorrow() {
        when(bookingRepo.findIdsByStatusAndDateRange(any(), any(), any())).thenReturn(List.of(7L));

        assertEquals(List.of(7L), service.findReminderCandidateIds());
        verify(bookingRepo).findIdsByStatusAndDateRange(BookingStatus.ACCEPTED, TODAY, TODAY.plusDays(1));
    }

    @Test
    void sendsTheTwoHourReminderForAnAppointmentSoon() {
        Booking booking = booking(BookingStatus.ACCEPTED, TODAY, NOW.minusDays(2));
        booking.setBookingTime(LocalTime.of(11, 30));
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        assertEquals(BookingTimePolicy.ReminderType.SOON, service.sendDueReminder(30L));
        verify(notificationService).bookingReminder(booking, BookingTimePolicy.ReminderType.SOON, TODAY);
    }

    @Test
    void sendsTheDayReminderForAnAppointmentTomorrow() {
        Booking booking = booking(BookingStatus.ACCEPTED, TODAY.plusDays(1), NOW.minusDays(2));
        booking.setBookingTime(LocalTime.of(9, 30));
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        assertEquals(BookingTimePolicy.ReminderType.DAY, service.sendDueReminder(30L));
        verify(notificationService).bookingReminder(booking, BookingTimePolicy.ReminderType.DAY, TODAY);
    }

    @Test
    void noReminderForACancelledOrFarAppointment() {
        Booking cancelled = booking(BookingStatus.CANCELLED, TODAY, NOW.minusDays(2));
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(cancelled));
        assertNull(service.sendDueReminder(30L));

        Booking far = booking(BookingStatus.ACCEPTED, TODAY.plusDays(1), NOW.minusDays(2));
        far.setBookingTime(LocalTime.of(18, 0));
        when(bookingRepo.findById(31L)).thenReturn(Optional.of(far));
        assertNull(service.sendDueReminder(31L));

        verify(notificationService, never()).bookingReminder(any(), any(), any());
    }

    private Booking booking(BookingStatus status, LocalDate date, LocalDateTime createdAt) {
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setStatus(status);
        booking.setBookingDate(date);
        booking.setBookingTime(LocalTime.of(14, 0));
        booking.setTotalDurationMinutes(30);
        booking.setCreatedAt(createdAt);
        return booking;
    }
}
