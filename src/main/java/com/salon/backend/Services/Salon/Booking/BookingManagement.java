package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.AssignableEmployeeResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingActionRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.CreateBookingRequest;

import java.time.LocalDate;
import java.util.List;

public interface BookingManagement {

    ApiResponse<BookingResponse> createBooking(CreateBookingRequest request, Long customerId);

    ApiResponse<List<BookingResponse>> getMyBookings(Long customerId);

    ApiResponse<List<BookingResponse>> getMySalonBookings(Long ownerId);

    /** The owner's salon bookings with an appointment date from {@code from} to {@code to}, both inclusive. */
    ApiResponse<List<BookingResponse>> getMySalonBookings(Long ownerId, LocalDate from, LocalDate to);

    ApiResponse<List<BookingResponse>> getMySalonPendingBookings(Long ownerId);

    ApiResponse<List<BookingResponse>> getMyEmployeeBookings(Long employeeId);

    ApiResponse<BookingResponse> acceptBooking(Long bookingId, BookingActionRequest request, Long ownerId);

    ApiResponse<BookingResponse> rejectBooking(Long bookingId, BookingActionRequest request, Long ownerId);

    ApiResponse<BookingResponse> cancelBooking(Long bookingId, Long customerId);

    /** The customer says they will come to a confirmed appointment (after a reminder). */
    ApiResponse<BookingResponse> confirmAttendance(Long bookingId, Long customerId);

    ApiResponse<BookingResponse> completeBooking(Long bookingId, Long ownerId);

    ApiResponse<BookingResponse> assignEmployee(Long bookingId, Long employeeId, Long ownerId);

    ApiResponse<BookingResponse> assignEmployee(
            Long bookingId,
            Long employeeId,
            String ownerNote,
            Long ownerId
    );

    ApiResponse<List<AssignableEmployeeResponse>> getAssignableEmployees(Long bookingId, Long ownerId);
}
