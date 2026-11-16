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
}
