package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.payments.PaymentTransactionType;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Payment.BookingPaymentRepo;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentServiceImplTest {

    /** 12:00 local time on 2026-11-16 (Amman is UTC+3). */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneId.of("Asia/Amman"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock private BookingRepo bookingRepo;
    @Mock private BookingPaymentRepo paymentRepo;
    @Mock private BookingNotificationService notificationService;

    private BookingPaymentServiceImpl service;
    private final BookingPaymentLifecycle lifecycle = new BookingPaymentLifecycle(new PaymentPolicy(0), CLOCK);
    private User customer;
    private User employee;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new BookingPaymentServiceImpl(bookingRepo, paymentRepo, notificationService, CLOCK);
        customer = user(1L, "Lina");
        employee = user(2L, "Sara");
        owner = user(3L, "Omar");
    }

    @Test
    void theEmployeeRecordsCashAfterTheServiceAndTheCustomerIsTold() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CASH);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingPaymentResponse> response = service.recordCashPayment(30L, 2L);

        assertTrue(response.isSuccess(), response.getMessage());
        BookingPayment payment = booking.getPayment();
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(NOW, payment.getPaidAt());
        assertEquals(employee, payment.getConfirmedBy());
        assertEquals("SH-10-000077", payment.getReceiptNumber());
        assertEquals(PaymentTransactionType.CASH_RECEIVED, payment.getTransactions().get(0).getType());
        assertEquals(19_500L, payment.getTransactions().get(0).getAmountFils());
        assertEquals("booking:30:cash", payment.getTransactions().get(0).getIdempotencyKey());
        assertEquals("EMPLOYEE", response.getData().viewerRole());
        assertFalse(response.getData().collectable());
        assertNull(response.getData().commissionFils(), "Only the owner sees the commission");
        assertTrue(response.getData().transactions().isEmpty(), "Only the owner sees the ledger");
        verify(paymentRepo).save(payment);
        verify(notificationService).paymentReceived(booking);
    }

    @Test
    void recordingCashTwiceChangesNothing() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CASH);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertTrue(service.recordCashPayment(30L, 2L).isSuccess());
        ApiResponse<BookingPaymentResponse> again = service.recordCashPayment(30L, 3L);

        assertTrue(again.isSuccess());
        assertEquals("Cash payment was already recorded", again.getMessage());
        assertEquals(1, booking.getPayment().getTransactions().size());
        verify(notificationService, times(1)).paymentReceived(any());
    }

    @Test
    void aCardAfterServiceBookingPaidInCashKeepsAMethodChangeInTheLedger() {
        Booking booking = booking(BookingStatus.IN_PROGRESS, PaymentMethod.CARD_POSTPAID);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingPaymentResponse> response = service.recordCashPayment(30L, 3L);

        assertTrue(response.isSuccess());
        assertEquals(PaymentMethod.CASH, booking.getPayment().getMethod());
        assertEquals(PaymentTransactionType.METHOD_CHANGED, booking.getPayment().getTransactions().get(0).getType());
        assertEquals(PaymentTransactionType.CASH_RECEIVED, booking.getPayment().getTransactions().get(1).getType());
        assertEquals("OWNER", response.getData().viewerRole());
        assertEquals(0L, response.getData().commissionFils());
        assertEquals(19_500L, response.getData().netFils());
        assertEquals(2, response.getData().transactions().size());
    }

    @Test
    void cashCannotBeRecordedBeforeTheServiceStarts() {
        Booking booking = booking(BookingStatus.ACCEPTED, PaymentMethod.CASH);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingPaymentResponse> response = service.recordCashPayment(30L, 2L);

        assertFalse(response.isSuccess());
        assertEquals("Record the payment when the service starts or after it ends", response.getMessage());
        assertEquals(PaymentStatus.UNPAID, booking.getPayment().getStatus());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void aPrepaidCardBookingIsNeverCollectedInCash() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CARD_PREPAID);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertEquals("This booking is paid by card in advance", service.recordCashPayment(30L, 2L).getMessage());
    }

    @Test
    void aClosedBookingHasNothingToCollect() {
        Booking booking = booking(BookingStatus.NO_SHOW, PaymentMethod.CASH);
        lifecycle.closeWithoutPayment(booking, "Customer did not come", employee);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertFalse(service.recordCashPayment(30L, 2L).isSuccess());
        assertEquals(PaymentStatus.VOIDED, booking.getPayment().getStatus());
    }

    @Test
    void theCustomerAndStrangersCannotRecordCash() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CASH);
        when(bookingRepo.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        assertFalse(service.recordCashPayment(30L, 1L).isSuccess());
        assertFalse(service.recordCashPayment(30L, 99L).isSuccess());
        assertEquals(PaymentStatus.UNPAID, booking.getPayment().getStatus());
    }

    @Test
    void theCustomerSeesTheReceiptWithoutTheLedger() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CASH);
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        ApiResponse<BookingPaymentResponse> response = service.getPayment(30L, 1L);

        assertTrue(response.isSuccess());
        assertEquals("CUSTOMER", response.getData().viewerRole());
        assertEquals(1, response.getData().lines().size());
        assertEquals(19_500L, response.getData().lines().get(0).amountFils());
        assertFalse(response.getData().collectable());
        assertNull(response.getData().netFils());
        assertFalse(service.getPayment(30L, 99L).isSuccess(), "Strangers do not see the booking");
    }

    @Test
    void aBookingFromBeforePaymentsHasNoReceipt() {
        Booking booking = booking(BookingStatus.COMPLETED, PaymentMethod.CASH);
        booking.setPayment(null);
        when(bookingRepo.findById(30L)).thenReturn(Optional.of(booking));

        assertEquals("This booking was made before payments were recorded",
                service.getPayment(30L, 1L).getMessage());
    }

    private Booking booking(BookingStatus status, PaymentMethod method) {
        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Lamsa Salon");
        salon.setStatus(SalonStatus.Accepted);
        salon.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(30L);
        booking.setSalon(salon);
        booking.setCustomer(customer);
        booking.setAssignedEmployee(employee);
        booking.setStatus(status);
        booking.setBookingDate(LocalDate.of(2026, 11, 16));
        booking.setBookingTime(LocalTime.of(11, 0));
        BookingServiceItem item = new BookingServiceItem();
        item.setSourceServiceId(20L);
        item.setServiceName("Haircut and color");
        item.setPrice(new BigDecimal("19.50"));
        item.setDurationMinutes(75);
        booking.addServiceItem(item);
        booking.setTotalPrice(new BigDecimal("19.50"));
        lifecycle.open(booking, method).setId(77L);
        return booking;
    }

    private User user(Long id, String firstName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName("Test");
        return user;
    }
}
