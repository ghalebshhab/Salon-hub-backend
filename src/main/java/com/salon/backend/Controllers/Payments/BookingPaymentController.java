package com.salon.backend.Controllers.Payments;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Payment.BookingPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.salon.backend.DTOs.Payment.CliqPaymentRequest;
import com.salon.backend.DTOs.Payment.PaymentMethodChangeRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Payment and receipt of one booking. */
@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
@RequiredArgsConstructor
public class BookingPaymentController {

    private final BookingPaymentService paymentService;

    @GetMapping
    public ApiResponse<BookingPaymentResponse> getPayment(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return paymentService.getPayment(bookingId, currentUser.getId());
    }

    @PostMapping("/cash")
    public ApiResponse<BookingPaymentResponse> recordCash(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return paymentService.recordCashPayment(bookingId, currentUser.getId());
    }

    /** The salon owner confirms that the customer's CliQ transfer arrived. The body is optional. */
    @PostMapping("/cliq")
    public ApiResponse<BookingPaymentResponse> recordCliq(
            @PathVariable Long bookingId,
            @RequestBody(required = false) CliqPaymentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return paymentService.recordCliqPayment(
                bookingId, currentUser.getId(), request == null ? null : request.reference());
    }

    /** The customer chooses cash or CliQ for an unpaid booking. */
    @PutMapping("/method")
    public ApiResponse<BookingPaymentResponse> changeMethod(
            @PathVariable Long bookingId,
            @RequestBody PaymentMethodChangeRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return paymentService.changeMethod(
                bookingId, currentUser.getId(), request == null ? null : request.method());
    }
}
