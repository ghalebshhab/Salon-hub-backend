package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.Departure.DeparturePlanResponse;
import com.salon.backend.DTOs.Salon.Booking.Tracking.CustomerPositionRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Booking.Departure.DeparturePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Leave on time" for the customer: expected turn, drive time with traffic and leave alerts. */
@RestController
@RequestMapping("/api/bookings/{bookingId}/departure-plan")
@RequiredArgsConstructor
public class DeparturePlanController {

    private final DeparturePlanService departurePlanService;

    @GetMapping
    public ApiResponse<DeparturePlanResponse> getPlan(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return departurePlanService.getPlan(bookingId, currentUser.getId());
    }

    @PutMapping("/origin")
    public ApiResponse<DeparturePlanResponse> setOrigin(
            @PathVariable Long bookingId,
            @RequestBody CustomerPositionRequest origin,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return departurePlanService.setOrigin(bookingId, origin, currentUser.getId());
    }

    @DeleteMapping("/origin")
    public ApiResponse<DeparturePlanResponse> clearOrigin(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return departurePlanService.clearOrigin(bookingId, currentUser.getId());
    }
}
