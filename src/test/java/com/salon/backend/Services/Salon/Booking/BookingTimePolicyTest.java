package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTimePolicyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    @Test
    void theSalonHasOneDayToAnswerARequestForALaterAppointment() {
        Booking booking = booking(DAY, LocalTime.of(15, 0), LocalDateTime.of(2026, 10, 17, 9, 30));

        assertEquals(LocalDateTime.of(2026, 10, 18, 9, 30), BookingTimePolicy.responseDeadline(booking));
        assertFalse(BookingTimePolicy.isResponseOverdue(booking, LocalDateTime.of(2026, 10, 18, 9, 29)));
        assertTrue(BookingTimePolicy.isResponseOverdue(booking, LocalDateTime.of(2026, 10, 18, 9, 30)));
    }

    @Test
    void aRequestForASoonerAppointmentMustBeAnsweredBeforeItStarts() {
        Booking booking = booking(DAY, LocalTime.of(12, 0), LocalDateTime.of(2026, 10, 20, 8, 0));

        assertEquals(LocalDateTime.of(DAY, LocalTime.of(12, 0)), BookingTimePolicy.responseDeadline(booking));
    }

    @Test
    void aRequestWithoutACreationTimeUsesTheAppointmentStart() {
        Booking booking = booking(DAY, LocalTime.of(12, 0), null);

        assertEquals(LocalDateTime.of(DAY, LocalTime.of(12, 0)), BookingTimePolicy.responseDeadline(booking));
    }

    @Test
    void remindersAreDueOneDayAndTwoHoursBeforeTheStart() {
        Booking booking = booking(DAY, LocalTime.of(15, 0), null);

        assertNull(BookingTimePolicy.dueReminder(booking, LocalDateTime.of(2026, 10, 19, 14, 59)));
        assertEquals(BookingTimePolicy.ReminderType.DAY,
                BookingTimePolicy.dueReminder(booking, LocalDateTime.of(2026, 10, 19, 15, 0)));
        assertEquals(BookingTimePolicy.ReminderType.DAY,
                BookingTimePolicy.dueReminder(booking, LocalDateTime.of(DAY, LocalTime.of(12, 59))));
        assertEquals(BookingTimePolicy.ReminderType.SOON,
                BookingTimePolicy.dueReminder(booking, LocalDateTime.of(DAY, LocalTime.of(13, 0))));
        assertNull(BookingTimePolicy.dueReminder(booking, LocalDateTime.of(DAY, LocalTime.of(15, 0))));
    }

    @Test
    void aNoShowCanBeRecordedFifteenMinutesAfterTheStart() {
        Booking booking = booking(DAY, LocalTime.of(15, 0), null);

        assertEquals(LocalDateTime.of(DAY, LocalTime.of(15, 15)), BookingTimePolicy.noShowAllowedFrom(booking));
    }

    private Booking booking(LocalDate date, LocalTime time, LocalDateTime createdAt) {
        Booking booking = new Booking();
        booking.setBookingDate(date);
        booking.setBookingTime(time);
        booking.setCreatedAt(createdAt);
        return booking;
    }
}
