package com.salon.backend.Entities.salons.bookings;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Live state of an accepted appointment: the customer's trip to the salon and
 * the employee's progress. Customer coordinates are kept only while the
 * customer is on the way and are cleared on arrival, completion or cancel.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class BookingTracking {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "journey_status", length = 20)
    private JourneyStatus journeyStatus;

    @Column(name = "journey_started_at")
    private LocalDateTime journeyStartedAt;

    @Column(name = "customer_latitude")
    private Double customerLatitude;

    @Column(name = "customer_longitude")
    private Double customerLongitude;

    @Column(name = "customer_location_updated_at")
    private LocalDateTime customerLocationUpdatedAt;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "service_started_at")
    private LocalDateTime serviceStartedAt;

    @Column(name = "expected_end_at")
    private LocalDateTime expectedEndAt;

    /**
     * Where the customer will leave from, for "Leave on time" alerts. Set only
     * when the customer turns the alerts on; cleared when the trip starts
     * sharing live, on arrival, completion, or when the customer turns it off.
     */
    @Column(name = "plan_latitude")
    private Double planLatitude;

    @Column(name = "plan_longitude")
    private Double planLongitude;

    @Column(name = "plan_updated_at")
    private LocalDateTime planUpdatedAt;

    public JourneyStatus journeyStatusOrDefault() {
        return journeyStatus == null ? JourneyStatus.NOT_STARTED : journeyStatus;
    }

    public void clearCustomerPosition() {
        customerLatitude = null;
        customerLongitude = null;
        distanceMeters = null;
        etaMinutes = null;
        clearDepartureOrigin();
    }

    public boolean hasDepartureOrigin() {
        return planLatitude != null && planLongitude != null;
    }

    public void clearDepartureOrigin() {
        planLatitude = null;
        planLongitude = null;
        planUpdatedAt = null;
    }
}
