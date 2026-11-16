package com.salon.backend.Entities.salons.bookings;

public enum BookingStatus {
    PENDING,
    ACCEPTED,
    /** The assigned employee started the service. */
    IN_PROGRESS,
    REJECTED,
    CANCELLED,
    COMPLETED,
    /** The salon did not answer a pending request in time. */
    EXPIRED,
    /** The customer did not come to a confirmed appointment. */
    NO_SHOW
}
