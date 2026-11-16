package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.BookingTrackingResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.DTOs.Salon.Booking.Tracking.ExtendServiceRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Booking.Tracking.BookingTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Live appointment tracking for the customer, the assigned employee and the owner. */
@RestController
@RequestMapping("/api/bookings/{bookingId}/tracking")
@RequiredArgsConstructor
public class BookingTrackingController {

    private final BookingTrackingService trackingService;

    @GetMapping
    public ApiResponse<BookingTrackingResponse> getTracking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.getTracking(bookingId, currentUser.getId());
    }

    @PostMapping("/journey/start")
    public ApiResponse<BookingTrackingResponse> startJourney(
            @PathVariable Long bookingId,
            @RequestBody(required = false) CustomerPositionRequest position,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.startJourney(bookingId, position, currentUser.getId());
    }

    @PutMapping("/journey/position")
    public ApiResponse<BookingTrackingResponse> updatePosition(
            @PathVariable Long bookingId,
            @RequestBody CustomerPositionRequest position,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.updatePosition(bookingId, position, currentUser.getId());
    }

    @PostMapping("/journey/arrived")
    public ApiResponse<BookingTrackingResponse> markArrived(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.markArrived(bookingId, currentUser.getId());
    }

    @PostMapping("/journey/stop")
    public ApiResponse<BookingTrackingResponse> stopJourney(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.stopJourney(bookingId, currentUser.getId());
    }

    @PostMapping("/service/start")
    public ApiResponse<BookingTrackingResponse> startService(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.startService(bookingId, currentUser.getId());
    }

    @PostMapping("/service/extend")
    public ApiResponse<BookingTrackingResponse> extendService(
            @PathVariable Long bookingId,
            @RequestBody ExtendServiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.extendService(bookingId, request, currentUser.getId());
    }

    @PostMapping("/no-show")
    public ApiResponse<BookingTrackingResponse> markNoShow(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.markNoShow(bookingId, currentUser.getId());
    }

    @PostMapping("/service/complete")
    public ApiResponse<BookingTrackingResponse> completeService(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trackingService.completeService(bookingId, currentUser.getId());
    }
}
