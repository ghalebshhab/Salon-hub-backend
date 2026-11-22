package com.salon.backend.Services.Salon.Booking.Tracking;

import java.time.LocalDateTime;

/**
 * Estimates the trip from the customer to the salon. The offline default works
 * from straight-line distance; {@link GoogleRoutesTravelTimeEstimator} replaces
 * it with live and predicted traffic when a Google Maps key is configured.
 */
public interface TravelTimeEstimator {

    String DISTANCE_SOURCE = "DISTANCE_ESTIMATE";

    TravelEstimate estimate(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude);

    /**
     * Drive time for a trip that starts at {@code departAt} (null means now).
     * The default has no traffic data, so the time is the same at every hour.
     */
    default RouteEstimate route(double fromLatitude, double fromLongitude,
                                double toLatitude, double toLongitude,
                                LocalDateTime departAt) {
        TravelEstimate estimate = estimate(fromLatitude, fromLongitude, toLatitude, toLongitude);
        return new RouteEstimate(estimate.distanceMeters(), estimate.etaMinutes(),
                estimate.etaMinutes(), DISTANCE_SOURCE);
    }

    /** Where the minutes come from, for the API responses. */
    default String sourceName() {
        return DISTANCE_SOURCE;
    }

    record TravelEstimate(int distanceMeters, int etaMinutes) {
    }

    /**
     * @param minutes         drive time with the traffic expected at the departure time
     * @param freeFlowMinutes drive time on empty roads
     */
    record RouteEstimate(int distanceMeters, int minutes, int freeFlowMinutes, String source) {

        public int trafficDelayMinutes() {
            return Math.max(0, minutes - freeFlowMinutes);
        }

        /** LIGHT, MODERATE or HEAVY; null when the source has no traffic data. */
        public String trafficLevel() {
            if (DISTANCE_SOURCE.equals(source)) {
                return null;
            }
            int delay = trafficDelayMinutes();
            double ratio = freeFlowMinutes <= 0 ? 1 : (double) minutes / freeFlowMinutes;
            if (delay <= 2 || ratio < 1.15) {
                return "LIGHT";
            }
            return ratio < 1.4 ? "MODERATE" : "HEAVY";
        }
    }
}
