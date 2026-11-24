package com.salon.backend.DTOs.Payment;

/** The owner confirms a CliQ transfer. {@code reference} is the optional bank reference. */
public record CliqPaymentRequest(String reference) {
}
