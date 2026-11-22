package com.salon.backend.DTOs.Salon.Booking.Departure;

import java.time.LocalDateTime;

/**
 * "Leave on time" for the customer of one booking.
 * <ul>
 *   <li>{@code phase}: PLANNED_DAY, WAIT, LEAVE_SOON, LEAVE_NOW, RUNNING_LATE,
 *       ON_THE_WAY, ARRIVED, IN_SERVICE or CLOSED.</li>
 *   <li>{@code confidence}: SCHEDULED (no live queue yet), LOW, MEDIUM or HIGH.</li>
 *   <li>Travel fields are null until the customer shares where they leave from.</li>
 *   <li>{@code trafficLevel}: LIGHT, MODERATE, HEAVY, or null without traffic data.</li>
 *   <li>{@code travelSource}: GOOGLE_TRAFFIC or DISTANCE_ESTIMATE.</li>
 * </ul>
 * Other clients are never named; only how many are ahead.
 */
public record DeparturePlanResponse(
        Long bookingId,
        String phase,
        String salonName,
        LocalDateTime slotStart,
        LocalDateTime expectedStartAt,
        int delayMinutes,
        int clientsAhead,
        String confidence,
        Integer travelMinutes,
        Integer freeFlowMinutes,
        Integer trafficDelayMinutes,
        String trafficLevel,
        Integer distanceMeters,
        String travelSource,
        int bufferMinutes,
        LocalDateTime arriveBy,
        LocalDateTime leaveAt,
        Integer minutesUntilLeave,
        boolean alertsOn,
        LocalDateTime originUpdatedAt,
        LocalDateTime serverTime
) {
}
