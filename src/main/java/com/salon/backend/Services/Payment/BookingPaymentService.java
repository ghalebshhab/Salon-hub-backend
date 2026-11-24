package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.BookingPaymentResponse;
import com.salon.backend.Entities.payments.PaymentMethod;

public interface BookingPaymentService {

    /** Receipt and payment state for the customer, the assigned employee or the owner. */
    ApiResponse<BookingPaymentResponse> getPayment(Long bookingId, Long viewerId);

    /** The assigned employee or the owner records cash received. Repeating it changes nothing. */
    ApiResponse<BookingPaymentResponse> recordCashPayment(Long bookingId, Long staffId);

    /** The salon owner confirms that the customer's CliQ transfer arrived. Repeating it changes nothing. */
    ApiResponse<BookingPaymentResponse> recordCliqPayment(Long bookingId, Long ownerId, String reference);

    /** The customer switches an unpaid booking between cash and CliQ before the service starts. */
    ApiResponse<BookingPaymentResponse> changeMethod(Long bookingId, Long customerId, PaymentMethod method);
}
