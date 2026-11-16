package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse;

public interface BookingPaymentService {

    /** Receipt and payment state for the customer, the assigned employee or the owner. */
    ApiResponse<BookingPaymentResponse> getPayment(Long bookingId, Long viewerId);

    /** The assigned employee or the owner records cash received. Repeating it changes nothing. */
    ApiResponse<BookingPaymentResponse> recordCashPayment(Long bookingId, Long staffId);
}
