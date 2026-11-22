package com.salon.backend.Services.Salon.Booking.Tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drive time from the Google Routes API with live traffic (departure now) or
 * predicted traffic (a later departure). Active only when
 * {@code google.maps.api-key} is set; without it the distance estimate is used.
 *
 * <p>Calls are cached by a ~200 m grid on both ends and a 5-minute departure
 * bucket, so many customers near each other and repeated polls cost one call.
 * Any error falls back to {@link DistanceTravelTimeEstimator}.
 */
@Slf4j
@Primary
@Component
@ConditionalOnExpression("!'${google.maps.api-key:}'.isBlank()")
public class GoogleRoutesTravelTimeEstimator implements TravelTimeEstimator {

    static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    static final String FIELD_MASK = "routes.duration,routes.staticDuration,routes.distanceMeters";
    static final String SOURCE = "GOOGLE_TRAFFIC";
    /** About 200 m in latitude. */
    static final double GRID_DEGREES = 0.002;
    static final Duration CACHE_TTL = Duration.ofMinutes(5);
    static final int MAX_CACHE_ENTRIES = 5_000;
    /** Google needs a departure time in the future; closer than this is sent as "now". */
    static final Duration MIN_FUTURE_DEPARTURE = Duration.ofMinutes(2);
    /** Same as the distance estimate: parking and walking to the door. */
    static final int ARRIVAL_BUFFER_MINUTES = DistanceTravelTimeEstimator.ARRIVAL_BUFFER_MINUTES;
    static final int NEAR_METERS = 150;

    private final RestClient restClient;
    private final String apiKey;
    private final DistanceTravelTimeEstimator fallback;
    private final Clock clock;
    private final Map<String, CachedRoute> cache = new ConcurrentHashMap<>();

    public GoogleRoutesTravelTimeEstimator(
            @Value("${google.maps.api-key:}") String apiKey,
            DistanceTravelTimeEstimator fallback,
            Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.apiKey = apiKey.trim();
        this.fallback = fallback;
        this.clock = clock;
    }

    @Override
    public TravelEstimate estimate(double fromLatitude, double fromLongitude,
                                   double toLatitude, double toLongitude) {
        RouteEstimate route = route(fromLatitude, fromLongitude, toLatitude, toLongitude, null);
        return new TravelEstimate(route.distanceMeters(), route.minutes());
    }

    @Override
    public RouteEstimate route(double fromLatitude, double fromLongitude,
                               double toLatitude, double toLongitude,
                               LocalDateTime departAt) {
        double straight = DistanceTravelTimeEstimator.haversineMeters(
                fromLatitude, fromLongitude, toLatitude, toLongitude);
        if (straight < NEAR_METERS) {
            return fallback.route(fromLatitude, fromLongitude, toLatitude, toLongitude, departAt);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime departure = departAt != null && departAt.isAfter(now.plus(MIN_FUTURE_DEPARTURE))
                ? departAt
                : null;
        String key = cacheKey(fromLatitude, fromLongitude, toLatitude, toLongitude, departure == null ? now : departure);
        CachedRoute cached = cache.get(key);
        if (cached != null && cached.storedAt().plus(CACHE_TTL).isAfter(now)) {
            return cached.route();
        }
        try {
            RouteEstimate route = callGoogle(fromLatitude, fromLongitude, toLatitude, toLongitude, departure);
            if (cache.size() >= MAX_CACHE_ENTRIES) {
                cache.clear();
            }
            cache.put(key, new CachedRoute(route, now));
            return route;
        } catch (RuntimeException exception) {
            log.warn("Google Routes call failed, using the distance estimate: {}", exception.getMessage());
            return fallback.route(fromLatitude, fromLongitude, toLatitude, toLongitude, departAt);
        }
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    private RouteEstimate callGoogle(double fromLatitude, double fromLongitude,
                                     double toLatitude, double toLongitude,
                                     LocalDateTime departure) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", waypoint(fromLatitude, fromLongitude));
        body.put("destination", waypoint(toLatitude, toLongitude));
        body.put("travelMode", "DRIVE");
        body.put("routingPreference", "TRAFFIC_AWARE_OPTIMAL");
        if (departure != null) {
            body.put("departureTime", departure.atZone(clock.getZone()).toInstant().toString());
        }
        Map<?, ?> response = restClient.post()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("routes") instanceof List<?> routes)
                || routes.isEmpty() || !(routes.get(0) instanceof Map<?, ?> first)) {
            throw new IllegalStateException("no route found");
        }
        long seconds = parseSeconds(first.get("duration"));
        long freeSeconds = first.get("staticDuration") == null ? seconds : parseSeconds(first.get("staticDuration"));
        int distance = first.get("distanceMeters") instanceof Number number ? number.intValue() : 0;
        int minutes = (int) Math.ceil(seconds / 60.0) + ARRIVAL_BUFFER_MINUTES;
        int freeMinutes = (int) Math.ceil(freeSeconds / 60.0) + ARRIVAL_BUFFER_MINUTES;
        return new RouteEstimate(distance, minutes, Math.min(minutes, freeMinutes), SOURCE);
    }

    private static Map<String, Object> waypoint(double latitude, double longitude) {
        return Map.of("location", Map.of("latLng", Map.of("latitude", latitude, "longitude", longitude)));
    }

    /** Google sends durations as strings such as "1250s". */
    static long parseSeconds(Object value) {
        if (value == null) {
            throw new IllegalStateException("route has no duration");
        }
        String text = value.toString().trim();
        if (text.endsWith("s")) {
            text = text.substring(0, text.length() - 1);
        }
        return Math.round(Double.parseDouble(text));
    }

    private static String cacheKey(double fromLatitude, double fromLongitude,
                                   double toLatitude, double toLongitude, LocalDateTime departure) {
        long bucket = departure.withSecond(0).withNano(0).toLocalTime().toSecondOfDay() / CACHE_TTL.toSeconds();
        return cell(fromLatitude) + ":" + cell(fromLongitude) + ">" + cell(toLatitude) + ":" + cell(toLongitude)
                + "@" + departure.toLocalDate() + "#" + bucket;
    }

    private static long cell(double degrees) {
        return Math.round(degrees / GRID_DEGREES);
    }

    private record CachedRoute(RouteEstimate route, LocalDateTime storedAt) {
    }
}
