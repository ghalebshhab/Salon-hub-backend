package com.salon.backend.Services.Auth.Security;

import com.salon.backend.Entities.users.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtYXQtbGVhc3QtMzItYnl0ZXM=";

    private final JwtService jwtService = new JwtService(TEST_SECRET);

    @Test
    void tokenUsesImmutableUserIdAsSubjectAndCarriesTheSessionId() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");

        String token = jwtService.generateAccessToken(user, "session-1", Duration.ofMinutes(15));

        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("session-1", jwtService.extractSessionId(token));
        assertTrue(jwtService.isTokenValid(token, 42L));
        assertFalse(jwtService.isTokenValid(token, 43L));
    }

    @Test
    void credentialChangeInvalidatesTokensIssuedBeforeIt() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");

        String token = jwtService.generateAccessToken(user, "session-1", Duration.ofMinutes(15));
        Instant issuedAt = jwtService.extractIssuedAt(token);

        assertTrue(jwtService.isTokenValid(token, 42L, issuedAt));
        assertFalse(jwtService.isTokenValid(token, 42L, issuedAt.plusSeconds(1)));
    }
}
