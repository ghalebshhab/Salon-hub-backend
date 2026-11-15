package com.salon.backend.Services.Salon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Locale;

/**
 * Validates the map point an owner selects for a salon.
 * The coordinates are the source of truth: the stored Google Maps link is always
 * built from them, so the public link can never point somewhere else than the pin.
 */
public final class SalonMapLocation {

    static final double JORDAN_SOUTH = 29.0;
    static final double JORDAN_NORTH = 33.5;
    static final double JORDAN_WEST = 34.8;
    static final double JORDAN_EAST = 39.4;
    private static final int COORDINATE_SCALE = 7;

    private SalonMapLocation() {
    }

    public record Result(String error, String googleMapsUrl, Double latitude, Double longitude) {
        public boolean isValid() {
            return error == null;
        }

        static Result failure(String error) {
            return new Result(error, null, null, null);
        }

        static Result empty() {
            return new Result(null, null, null, null);
        }
    }

    public static Result resolve(String mapsUrl, Double latitude, Double longitude) {
        boolean hasUrl = mapsUrl != null && !mapsUrl.isBlank();
        if (latitude == null && longitude == null) {
            return hasUrl
                    ? Result.failure("Select the salon location on the map so the pin can be saved")
                    : Result.empty();
        }
        if (latitude == null || longitude == null) {
            return Result.failure("Latitude and longitude must be provided together");
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return Result.failure("The selected coordinates are not valid");
        }
        if (latitude < JORDAN_SOUTH || latitude > JORDAN_NORTH
                || longitude < JORDAN_WEST || longitude > JORDAN_EAST) {
            return Result.failure("The selected salon location must be inside Jordan");
        }
        if (hasUrl && !isGoogleMapsUrl(mapsUrl)) {
            return Result.failure("Enter a valid HTTPS Google Maps location URL");
        }

        double lat = round(latitude);
        double lng = round(longitude);
        return new Result(null, canonicalUrl(lat, lng), lat, lng);
    }

    static String canonicalUrl(double latitude, double longitude) {
        return "https://www.google.com/maps/search/?api=1&query="
                + plain(latitude) + "," + plain(longitude);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static boolean isGoogleMapsUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return false;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            return host.equals("google.com") || host.endsWith(".google.com")
                    || host.equals("goo.gl") || host.endsWith(".goo.gl");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
