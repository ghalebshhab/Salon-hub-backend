package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.salons.bookings.Booking;
public interface BookingNotificationService {

    void bookingAccepted(Booking booking);

    void bookingRejected(Booking booking);

    void bookingEmployeeReassigned(Booking booking, Long previousEmployeeId);

    void bookingCancelled(Booking booking);
}
