package com.salon.backend.Services.Auth.Security;

import com.salon.backend.Entities.users.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

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
}
