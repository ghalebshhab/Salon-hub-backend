package com.salon.backend.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how often one client can call the public auth endpoints. This stops
 * password guessing on login and email flooding on the code endpoints.
 *
 * <p>The counters live in memory, which is correct for one backend instance.
 * With more than one instance behind a load balancer, move them to Redis
 * (for example with Bucket4j).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private record Rule(int maxRequests, Duration window) {}

    private static final Rule LOGIN = new Rule(10, Duration.ofMinutes(5));
    private static final Rule SEND_EMAIL = new Rule(5, Duration.ofMinutes(15));
    private static final Rule CHECK_CODE = new Rule(15, Duration.ofMinutes(15));

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", LOGIN,
            "/api/dashboard/login", LOGIN,
            "/api/auth/register", SEND_EMAIL,
            "/api/auth/rigester", SEND_EMAIL,
            "/api/auth/email/resend", SEND_EMAIL,
            "/api/auth/password/forgot", SEND_EMAIL,
            "/api/auth/email/verify", CHECK_CODE,
            "/api/auth/password/verify-code", CHECK_CODE,
            "/api/auth/password/reset", CHECK_CODE);

    private static final int CLEANUP_EVERY = 500;
    private static final String TOO_MANY_BODY =
            "{\"success\":false,\"message\":\"Too many attempts. Please wait a few minutes and try again.\",\"data\":null}";

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();
    private int requestsSinceCleanup;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !RULES.containsKey(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        Rule rule = RULES.get(path);
        Instant now = clock.instant();
        cleanupSometimes(now);

        Deque<Instant> times = hits.computeIfAbsent(clientIp(request) + "|" + path, key -> new ArrayDeque<>());
        long retryAfterSeconds;
        synchronized (times) {
            dropOld(times, now, rule.window());
            if (times.size() < rule.maxRequests()) {
                times.addLast(now);
                retryAfterSeconds = 0;
            } else {
                retryAfterSeconds = Math.max(1, Duration.between(now, times.peekFirst().plus(rule.window())).toSeconds());
            }
        }

        if (retryAfterSeconds > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(TOO_MANY_BODY);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * The real client IP. Behind the host proxy (Render) the last
     * X-Forwarded-For entry is the address the proxy saw; earlier entries can
     * be set by the client, so they are not trusted.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private static void dropOld(Deque<Instant> times, Instant now, Duration window) {
        Instant oldest = now.minus(window);
        while (!times.isEmpty() && times.peekFirst().isBefore(oldest)) times.pollFirst();
    }

    /** Remove idle clients now and then, so the map does not grow forever. */
    private void cleanupSometimes(Instant now) {
        synchronized (this) {
            if (++requestsSinceCleanup < CLEANUP_EVERY) return;
            requestsSinceCleanup = 0;
        }
        hits.entrySet().removeIf(entry -> {
            Deque<Instant> times = entry.getValue();
            synchronized (times) {
                dropOld(times, now, Duration.ofMinutes(15));
                return times.isEmpty();
            }
        });
    }
}
