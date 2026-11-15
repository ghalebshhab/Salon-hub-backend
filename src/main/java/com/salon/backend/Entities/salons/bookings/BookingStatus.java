package com.salon.backend.Entities.salons.bookings;

public enum BookingStatus {
    PENDING,
    ACCEPTED,
    /** The assigned employee started the service. */
    IN_PROGRESS,
    REJECTED,
    CANCELLED,
    COMPLETED
}
