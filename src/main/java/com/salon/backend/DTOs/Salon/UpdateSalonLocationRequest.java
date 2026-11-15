package com.salon.backend.DTOs.Salon;

/**
 * Sets or clears only the salon map pin. Send both coordinates as null to remove it.
 * {@code city} is optional; a supported Jordan city detected from the map replaces
 * the profile city, anything else is ignored.
 */
public record UpdateSalonLocationRequest(Double latitude, Double longitude, String city) {
}
