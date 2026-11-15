package com.salon.backend.Services.Notification;

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
}
