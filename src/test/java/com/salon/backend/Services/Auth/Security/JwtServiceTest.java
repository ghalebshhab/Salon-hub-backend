package com.salon.backend.Services.Auth.Security;

import com.salon.backend.Entities.users.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtYXQtbGVhc3QtMzItYnl0ZXM=";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 86_400_000L);

    @Test
    void tokenUsesImmutableUserIdAsSubjectAndKeepsEmailAsClaim() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");

        String token = jwtService.generateToken(user);

        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, 42L));
        assertFalse(jwtService.isTokenValid(token, 43L));
    }

    @Test
    void credentialChangeInvalidatesTokensIssuedBeforeIt() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");

        String token = jwtService.generateToken(user);
        Instant issuedAt = jwtService.extractIssuedAt(token);

        assertTrue(jwtService.isTokenValid(token, 42L, issuedAt));
        assertFalse(jwtService.isTokenValid(token, 42L, issuedAt.plusSeconds(1)));
    }
}
