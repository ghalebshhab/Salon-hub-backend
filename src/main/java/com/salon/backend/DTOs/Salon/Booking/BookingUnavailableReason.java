package com.salon.backend.DTOs.Salon.Booking;

/** Why an employee or a whole day has no free appointment time. */
public final class BookingUnavailableReason {

    public static final String TIME_OFF = "TIME_OFF";
    public static final String NOT_WORKING = "NOT_WORKING";
    public static final String FULLY_BOOKED = "FULLY_BOOKED";
    public static final String BUSY = "BUSY";
    public static final String NO_TIME_LEFT_TODAY = "NO_TIME_LEFT_TODAY";
    public static final String NO_QUALIFIED_EMPLOYEE = "NO_QUALIFIED_EMPLOYEE";

    private BookingUnavailableReason() {
    }
}
