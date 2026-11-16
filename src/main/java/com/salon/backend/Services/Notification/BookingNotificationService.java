package com.salon.backend.Services.Notification;

import com.salon.backend.Services.Salon.Booking.BookingTimePolicy;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
public interface BookingNotificationService {

    void bookingAccepted(Booking booking);

    void bookingRejected(Booking booking);

    void bookingEmployeeReassigned(Booking booking, Long previousEmployeeId);

    void bookingCancelled(Booking booking);

    void bookingRescheduleProposed(Booking booking, BookingRescheduleProposal proposal);

    void bookingRescheduleAccepted(Booking booking, BookingRescheduleProposal proposal);

    void bookingRescheduleRejected(Booking booking, BookingRescheduleProposal proposal);

    /** Tells the salon owner a customer sent a new request. */
    void bookingRequested(Booking booking);

    /** Tells the assigned employee they will serve this confirmed appointment. */
    void bookingAssignedToEmployee(Booking booking);

    void customerOnTheWay(Booking booking);

    void customerArrived(Booking booking);

    void serviceStarted(Booking booking);

    void bookingCompleted(Booking booking);

    /** Tells the customer and the owner that a request expired without an answer. */
    void bookingExpired(Booking booking);

    /**
     * Reminds the customer of a confirmed appointment. The two-hour reminder
     * also goes to the assigned employee. One reminder of each type per booking.
     */
    void bookingReminder(Booking booking, BookingTimePolicy.ReminderType type, java.time.LocalDate today);

    /** Tells the customer that the salon recorded a no-show. */
    void bookingNoShow(Booking booking);

    /** Tells the customer that the salon received the money; the receipt is ready. */
    void paymentReceived(Booking booking);
}
