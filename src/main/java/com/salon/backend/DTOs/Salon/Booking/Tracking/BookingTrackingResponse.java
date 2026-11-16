package com.salon.backend.DTOs.Salon.Booking.Tracking;

import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.JourneyStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Live view of one appointment, shaped for the viewer.
 * <ul>
 *   <li>{@code viewerRole}: CUSTOMER, EMPLOYEE or OWNER.</li>
 *   <li>{@code customerLatitude}/{@code customerLongitude}: only for EMPLOYEE and OWNER,
 *       only while the customer is on the way.</li>
 *   <li>{@code employeeState}: AWAITING_EMPLOYEE, AVAILABLE, BUSY_WITH_ANOTHER_CLIENT,
 *       SERVING_YOU, FINISHED. Another client's details are never exposed.</li>
 *   <li>{@code etaSource}: DISTANCE_ESTIMATE. A routing provider can be added later.</li>
 * </ul>
 */
public record BookingTrackingResponse(
        Long bookingId,
        String viewerRole,
        BookingStatus status,
        LocalDate bookingDate,
        LocalTime bookingTime,
        int totalDurationMinutes,
        String salonName,
        Double salonLatitude,
        Double salonLongitude,
        String customerName,
        String customerPhoneNumber,
        String employeeName,
        JourneyStatus journeyStatus,
        LocalDateTime journeyStartedAt,
        Integer distanceMeters,
        Integer etaMinutes,
        LocalDateTime expectedArrivalAt,
        LocalDateTime locationUpdatedAt,
        LocalDateTime arrivedAt,
        Double customerLatitude,
        Double customerLongitude,
        String etaSource,
        String employeeState,
        LocalDateTime employeeBusyUntil,
        LocalDateTime serviceStartedAt,
        LocalDateTime expectedEndAt,
        Integer remainingMinutes,
        Integer progressPercent,
        LocalDateTime completedAt,
        boolean canShareJourney,
        boolean canStartService,
        boolean canCompleteService,
        boolean canMarkNoShow,
        LocalDateTime noShowAllowedFrom,
        LocalDateTime serverTime
) {
}
