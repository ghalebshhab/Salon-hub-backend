package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse.PaymentTransactionResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse.ReceiptLineResponse;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.payments.PaymentTransactionType;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Payment.BookingPaymentRepo;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import com.salon.backend.Services.Salon.Booking.BookingResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingPaymentServiceImpl implements BookingPaymentService {

    static final String CUSTOMER = "CUSTOMER";
    static final String EMPLOYEE = "EMPLOYEE";
    static final String OWNER = "OWNER";

    private final BookingRepo bookingRepo;
    private final BookingPaymentRepo paymentRepo;
    private final BookingNotificationService notificationService;
    private final Clock clock;
    private final PaymentSettingsService paymentSettingsService;

    static final int MAX_CLIQ_REFERENCE = 40;
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BookingPaymentResponse> getPayment(Long bookingId, Long viewerId) {
        Booking booking = bookingId == null ? null : bookingRepo.findById(bookingId).orElse(null);
        String role = roleOf(booking, viewerId);
        if (role == null) {
            return ApiResponse.error("Booking was not found");
        }
        if (booking.getPayment() == null) {
            return ApiResponse.error("This booking was made before payments were recorded");
        }
        return ApiResponse.success("Payment fetched successfully", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingPaymentResponse> recordCashPayment(Long bookingId, Long staffId) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        String role = roleOf(booking, staffId);
        if (role == null || CUSTOMER.equals(role)) {
            return ApiResponse.error("Booking of your salon was not found");
        }
        BookingPayment payment = booking.getPayment();
        if (payment == null) {
            return ApiResponse.error("This booking was made before payments were recorded");
        }
        if (payment.getMethod() == PaymentMethod.CASH && payment.getStatus() == PaymentStatus.PAID) {
            return ApiResponse.success("Cash payment was already recorded", toResponse(booking, role));
        }
        if (!PaymentRules.isCashCollectable(booking)) {
            return ApiResponse.error(collectError(booking));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        User actor = OWNER.equals(role) ? booking.getSalon().getOwner() : booking.getAssignedEmployee();
        if (payment.getMethod() != PaymentMethod.CASH) {
            payment.addTransaction(BookingPaymentLifecycle.transaction(
                    PaymentTransactionType.METHOD_CHANGED,
                    0,
                    "booking:" + booking.getId() + ":method:cash",
                    actor,
                    "Customer paid in cash instead of " + payment.getMethod(),
                    now
            ));
            payment.setMethod(PaymentMethod.CASH);
        }
        payment.moveTo(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setConfirmedBy(actor);
        payment.setUpdatedAt(now);
        payment.addTransaction(BookingPaymentLifecycle.transaction(
                PaymentTransactionType.CASH_RECEIVED,
                payment.getAmountFils(),
                "booking:" + booking.getId() + ":cash",
                actor,
                null,
                now
        ));
        paymentRepo.save(payment);
        if (payment.getReceiptNumber() == null) {
            payment.setReceiptNumber(PaymentPolicy.receiptNumber(booking.getSalon().getId(), payment.getId()));
        }
        notificationService.paymentReceived(booking);
        return ApiResponse.success("Cash payment recorded", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingPaymentResponse> recordCliqPayment(Long bookingId, Long ownerId, String reference) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        String role = roleOf(booking, ownerId);
        if (role == null || CUSTOMER.equals(role)) {
            return ApiResponse.error("Booking of your salon was not found");
        }
        if (!OWNER.equals(role)) {
            return ApiResponse.error("Only the salon owner can confirm a CliQ transfer");
        }
        BookingPayment payment = booking.getPayment();
        if (payment == null) {
            return ApiResponse.error("This booking was made before payments were recorded");
        }
        if (payment.getMethod() == PaymentMethod.CLIQ && payment.getStatus() == PaymentStatus.PAID) {
            return ApiResponse.success("CliQ payment was already recorded", toResponse(booking, role));
        }
        if (payment.getMethod() != PaymentMethod.CLIQ) {
            return ApiResponse.error("The customer did not choose CliQ for this booking");
        }
        if (!PaymentRules.isCliqConfirmable(booking)) {
            return ApiResponse.error(cliqError(booking));
        }
        String ref = reference == null || reference.isBlank() ? null : reference.trim();
        if (ref != null && ref.length() > MAX_CLIQ_REFERENCE) {
            return ApiResponse.error("The CliQ reference cannot be longer than " + MAX_CLIQ_REFERENCE + " characters");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        User owner = booking.getSalon().getOwner();
        payment.moveTo(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setConfirmedBy(owner);
        payment.setUpdatedAt(now);
        payment.addTransaction(BookingPaymentLifecycle.transaction(
                PaymentTransactionType.CLIQ_RECEIVED,
                payment.getAmountFils(),
                "booking:" + booking.getId() + ":cliq",
                owner,
                ref == null ? null : "CliQ reference " + ref,
                now
        ));
        paymentRepo.save(payment);
        if (payment.getReceiptNumber() == null) {
            payment.setReceiptNumber(PaymentPolicy.receiptNumber(booking.getSalon().getId(), payment.getId()));
        }
        notificationService.paymentReceived(booking);
        return ApiResponse.success("CliQ payment recorded", toResponse(booking, role));
    }

    @Override
    @Transactional
    public ApiResponse<BookingPaymentResponse> changeMethod(Long bookingId, Long customerId, PaymentMethod method) {
        Booking booking = bookingId == null ? null : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        if (!CUSTOMER.equals(roleOf(booking, customerId))) {
            return ApiResponse.error("Booking owned by you was not found");
        }
        if (method != PaymentMethod.CASH && method != PaymentMethod.CLIQ) {
            return ApiResponse.error("Choose cash or CliQ");
        }
        BookingPayment payment = booking.getPayment();
        if (payment == null) {
            return ApiResponse.error("This booking was made before payments were recorded");
        }
        if (payment.getMethod() == method) {
            return ApiResponse.success("Payment method is already " + method, toResponse(booking, CUSTOMER));
        }
        if (!PaymentRules.canChangeMethod(booking)) {
            if (payment.getMethod() != PaymentMethod.CASH && payment.getMethod() != PaymentMethod.CLIQ) {
                return ApiResponse.error("This booking is paid by card. Its payment method cannot change");
            }
            if (payment.getStatus() != PaymentStatus.UNPAID) {
                return ApiResponse.error("This booking is already paid or closed");
            }
            return ApiResponse.error("The payment method can change only before the service starts");
        }
        String methodError = paymentSettingsService.methodError(booking.getSalon(), method);
        if (methodError != null) {
            return ApiResponse.error(methodError);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        PaymentMethod previous = payment.getMethod();
        payment.addTransaction(BookingPaymentLifecycle.transaction(
                PaymentTransactionType.METHOD_CHANGED,
                0,
                "booking:" + booking.getId() + ":method:" + method.name().toLowerCase()
                        + ":" + payment.getTransactions().size(),
                booking.getCustomer(),
                "Customer changed " + previous + " to " + method,
                now
        ));
        payment.setMethod(method);
        if (method == PaymentMethod.CLIQ) {
            paymentSettingsService.copyCliqDetails(booking.getSalon(), payment);
        } else {
            payment.setCliqAlias(null);
            payment.setCliqName(null);
        }
        payment.setUpdatedAt(now);
        paymentRepo.save(payment);
        return ApiResponse.success(method == PaymentMethod.CLIQ
                ? "You will pay by CliQ"
                : "You will pay in cash at the salon", toResponse(booking, CUSTOMER));
    }

    private String cliqError(Booking booking) {
        BookingPayment payment = booking.getPayment();
        if (payment.getStatus().isSettled()) {
            return "This booking is already paid";
        }
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            return "This booking was closed without a service";
        }
        return "Confirm a CliQ transfer after the salon accepts the booking";
    }

    private String collectError(Booking booking) {
        BookingPayment payment = booking.getPayment();
        if (payment.getMethod() == PaymentMethod.CARD_PREPAID) {
            return "This booking is paid by card in advance";
        }
        if (payment.getStatus().isSettled()) {
            return "This booking is already paid";
        }
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            return "This booking was closed without a service";
        }
        return "Record the payment when the service starts or after it ends";
    }

    private String roleOf(Booking booking, Long viewerId) {
        if (booking == null || viewerId == null) {
            return null;
        }
        if (viewerId.equals(booking.getCustomer().getId())) {
            return CUSTOMER;
        }
        User employee = booking.getAssignedEmployee();
        if (employee != null && viewerId.equals(employee.getId())) {
            return EMPLOYEE;
        }
        User owner = booking.getSalon().getOwner();
        if (owner != null && viewerId.equals(owner.getId())) {
            return OWNER;
        }
        return null;
    }

    private BookingPaymentResponse toResponse(Booking booking, String role) {
        BookingPayment payment = booking.getPayment();
        boolean owner = OWNER.equals(role);
        boolean staff = !CUSTOMER.equals(role);
        List<ReceiptLineResponse> lines = booking.getServiceItems().stream()
                .map(item -> new ReceiptLineResponse(
                        item.getServiceName(),
                        PaymentPolicy.toFils(item.getPrice()),
                        item.getDurationMinutes()
                ))
                .toList();
        List<PaymentTransactionResponse> transactions = owner
                ? payment.getTransactions().stream()
                        .map(transaction -> new PaymentTransactionResponse(
                                transaction.getType().name(),
                                transaction.getAmountFils(),
                                transaction.getActor() == null
                                        ? "SalonHub"
                                        : BookingResponseMapper.fullName(transaction.getActor()),
                                transaction.getNote(),
                                transaction.getCreatedAt()
                        ))
                        .toList()
                : List.of();
        return new BookingPaymentResponse(
                booking.getId(),
                role,
                payment.getReceiptNumber(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmountFils(),
                payment.getRefundedFils(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getConfirmedBy() == null ? null : BookingResponseMapper.fullName(payment.getConfirmedBy()),
                booking.getSalon().getName(),
                BookingResponseMapper.fullName(booking.getCustomer()),
                booking.getBookingDate(),
                booking.getBookingTime(),
                lines,
                staff && PaymentRules.isCollectable(booking),
                owner ? payment.getCommissionFils() : null,
                owner ? payment.netFils() : null,
                transactions
        );
    }
}
