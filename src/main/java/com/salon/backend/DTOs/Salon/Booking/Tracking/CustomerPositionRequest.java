package com.salon.backend.DTOs.Salon.Booking.Tracking;

/** Customer's current position. Both values may be null when the customer declines to share. */
public record CustomerPositionRequest(Double latitude, Double longitude) {
}
