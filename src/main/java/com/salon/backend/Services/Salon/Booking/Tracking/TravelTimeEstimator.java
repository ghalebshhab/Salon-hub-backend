package com.salon.backend.Services.Salon.Booking.Tracking;

/**
 * Estimates the trip from the customer to the salon. The default implementation
 * works offline from straight-line distance; a routing API (for example Google
 * Distance Matrix) can replace it later without changing callers.
 */
public interface TravelTimeEstimator {

    TravelEstimate estimate(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude);

    record TravelEstimate(int distanceMeters, int etaMinutes) {
    }
}
