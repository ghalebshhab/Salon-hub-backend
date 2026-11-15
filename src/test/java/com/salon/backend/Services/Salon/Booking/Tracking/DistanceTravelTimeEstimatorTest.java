package com.salon.backend.Services.Salon.Booking.Tracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceTravelTimeEstimatorTest {

    private final DistanceTravelTimeEstimator estimator = new DistanceTravelTimeEstimator();

    @Test
    void crossTownAmmanTripTakesAboutTwentyMinutes() {
        // Sweifieh to Rainbow Street, about 6.5 km in a straight line.
        TravelTimeEstimator.TravelEstimate estimate =
                estimator.estimate(31.9566, 35.8620, 31.9508, 35.9241);

        assertThat(estimate.distanceMeters()).isBetween(5_500, 6_500);
        assertThat(estimate.etaMinutes()).isBetween(15, 25);
    }

    @Test
    void standingAtTheDoorMeansNoTravelTime() {
        TravelTimeEstimator.TravelEstimate estimate =
                estimator.estimate(31.9508, 35.9241, 31.9509, 35.9241);

        assertThat(estimate.distanceMeters()).isLessThan(20);
        assertThat(estimate.etaMinutes()).isZero();
    }
}
