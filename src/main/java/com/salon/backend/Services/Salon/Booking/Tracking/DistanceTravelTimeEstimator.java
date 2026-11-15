package com.salon.backend.Services.Salon.Booking.Tracking;

import org.springframework.stereotype.Component;

/**
 * Driving estimate from great-circle distance. Roads are longer than a straight
 * line ({@link #ROAD_FACTOR}) and city traffic is slow ({@link #CITY_SPEED_KMH}),
 * which gives realistic minutes for trips inside Amman and other Jordan cities.
 */
@Component
public class DistanceTravelTimeEstimator implements TravelTimeEstimator {

    static final double EARTH_RADIUS_METERS = 6_371_000;
    static final double ROAD_FACTOR = 1.35;
    static final double CITY_SPEED_KMH = 28;
    /** Parking and walking to the door. */
    static final int ARRIVAL_BUFFER_MINUTES = 2;

    @Override
    public TravelEstimate estimate(double fromLatitude, double fromLongitude,
                                   double toLatitude, double toLongitude) {
        double straight = haversineMeters(fromLatitude, fromLongitude, toLatitude, toLongitude);
        double roadMeters = straight * ROAD_FACTOR;
        double minutes = roadMeters / (CITY_SPEED_KMH * 1000 / 60);
        int eta = straight < 150 ? 0 : (int) Math.ceil(minutes) + ARRIVAL_BUFFER_MINUTES;
        return new TravelEstimate((int) Math.round(straight), eta);
    }

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
