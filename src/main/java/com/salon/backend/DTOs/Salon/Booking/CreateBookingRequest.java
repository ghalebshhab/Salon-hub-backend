package com.salon.backend.DTOs.Salon.Booking;

import com.salon.backend.Entities.payments.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateBookingRequest(
        Long salonId,
        List<Long> serviceIds,
        Long assignedEmployeeId,
        String customerPhoneNumber,
        String customerLocation,
        LocalDate bookingDate,
        LocalTime bookingTime,
        String note,
        /** Optional; the salon's default (cash) is used when it is missing. */
        PaymentMethod paymentMethod
) {

    /** Request without a payment choice, for callers made before payments existed. */
    public CreateBookingRequest(
            Long salonId,
            List<Long> serviceIds,
            Long assignedEmployeeId,
            String customerPhoneNumber,
            String customerLocation,
            LocalDate bookingDate,
            LocalTime bookingTime,
            String note) {
        this(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber,
                customerLocation, bookingDate, bookingTime, note, null);
    }
}
