package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingActionRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.AssignBookingEmployeeRequest;
import com.salon.backend.DTOs.Salon.Booking.CreateBookingRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Booking.BookingManagement;
import com.salon.backend.Services.Salon.Booking.BookingAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingManagement bookingManagement;
    private final BookingAvailabilityService bookingAvailabilityService;

    @PostMapping("/availability")
    public ApiResponse<BookingAvailabilityResponse> getAvailability(
            @RequestBody BookingAvailabilityRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingAvailabilityService.getAvailability(request, currentUser.getId());
    }

    @PostMapping
    public ApiResponse<BookingResponse> createBooking(
            @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.createBooking(request, currentUser.getId());
    }

    @GetMapping("/my")
    public ApiResponse<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.getMyBookings(currentUser.getId());
    }

    @GetMapping("/my-salon")
    public ApiResponse<List<BookingResponse>> getMySalonBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.getMySalonBookings(currentUser.getId());
    }

    @GetMapping("/my-salon/pending")
    public ApiResponse<List<BookingResponse>> getMySalonPendingBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.getMySalonPendingBookings(currentUser.getId());
    }

    @GetMapping("/employee/my")
    public ApiResponse<List<BookingResponse>> getMyEmployeeBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.getMyEmployeeBookings(currentUser.getId());
    }

    @PutMapping("/{bookingId}/accept")
    public ApiResponse<BookingResponse> acceptBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) BookingActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.acceptBooking(bookingId, request, currentUser.getId());
    }

    @PutMapping("/{bookingId}/reject")
    public ApiResponse<BookingResponse> rejectBooking(
            @PathVariable Long bookingId,
            @RequestBody BookingActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.rejectBooking(bookingId, request, currentUser.getId());
    }

    @PutMapping("/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.cancelBooking(bookingId, currentUser.getId());
    }

    @PutMapping("/{bookingId}/complete")
    public ApiResponse<BookingResponse> completeBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.completeBooking(bookingId, currentUser.getId());
    }

    @PutMapping("/{bookingId}/assign-employee/{employeeId}")
    public ApiResponse<BookingResponse> assignEmployee(
            @PathVariable Long bookingId,
            @PathVariable Long employeeId,
            @RequestBody(required = false) AssignBookingEmployeeRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bookingManagement.assignEmployee(
                bookingId,
                employeeId,
                request == null ? null : request.ownerNote(),
                currentUser.getId()
        );
    }
}
