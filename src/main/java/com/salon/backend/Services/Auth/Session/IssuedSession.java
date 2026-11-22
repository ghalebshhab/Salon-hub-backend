package com.salon.backend.Services.Auth.Session;

import java.time.Duration;

/**
 * Tokens for one signed-in device. The web layer writes them into httpOnly
 * cookies; they never appear in a response body for the web application.
 */
public record IssuedSession(
        String sessionId,
        String accessToken,
        Duration accessTtl,
        String refreshToken,
        Duration refreshTtl
) {
}
