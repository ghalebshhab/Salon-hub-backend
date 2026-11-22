package com.salon.backend.Services.Auth.Session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Writes and reads the sign-in cookies of the web application.
 *
 * <ul>
 *   <li>Access cookie: the short-lived JWT. httpOnly, so page scripts (and XSS) cannot read it.</li>
 *   <li>Refresh cookie: the rotating refresh token. httpOnly and sent only to {@code /api/auth}.</li>
 *   <li>Hint cookie: the value "1", readable by the page so it knows a session exists.
 *       It holds no secret.</li>
 * </ul>
 *
 * All cookies are {@code SameSite=Strict} and, outside local development, {@code Secure}.
 * In production the frontend proxies {@code /api} on its own domain, so the cookies are
 * first-party and other sites cannot send them (CSRF).
 */
@Component
public class AuthCookies {

    public static final String API_PATH = "/api";
    public static final String AUTH_PATH = "/api/auth";

    private final String accessName;
    private final String refreshName;
    private final String hintName;
    private final boolean secure;
    private final String sameSite;

    public AuthCookies(
            @Value("${salon.auth.cookie.access-name:salonhub_access}") String accessName,
            @Value("${salon.auth.cookie.refresh-name:salonhub_refresh}") String refreshName,
            @Value("${salon.auth.cookie.hint-name:salonhub_auth}") String hintName,
            @Value("${salon.auth.cookie.secure:true}") boolean secure,
            @Value("${salon.auth.cookie.same-site:Strict}") String sameSite
    ) {
        this.accessName = accessName;
        this.refreshName = refreshName;
        this.hintName = hintName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void write(HttpServletResponse response, IssuedSession session) {
        add(response, accessName, session.accessToken(), API_PATH, session.accessTtl(), true);
        add(response, refreshName, session.refreshToken(), AUTH_PATH, session.refreshTtl(), true);
        add(response, hintName, "1", "/", session.refreshTtl(), false);
    }

    public void clear(HttpServletResponse response) {
        add(response, accessName, "", API_PATH, Duration.ZERO, true);
        add(response, refreshName, "", AUTH_PATH, Duration.ZERO, true);
        add(response, hintName, "", "/", Duration.ZERO, false);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return read(request, accessName);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return read(request, refreshName);
    }

    /** True when the request carries any sign-in cookie (used by the CSRF header check). */
    public boolean hasSessionCookie(HttpServletRequest request) {
        return readAccessToken(request).isPresent() || readRefreshToken(request).isPresent();
    }

    private void add(HttpServletResponse response, String name, String value, String path,
                     Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
