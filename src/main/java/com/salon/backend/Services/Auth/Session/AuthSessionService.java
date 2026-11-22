package com.salon.backend.Services.Auth.Session;

import com.salon.backend.Entities.auth.SessionClient;
import com.salon.backend.Entities.users.User;

import java.util.Optional;

/** Server-side sign-in sessions: start, refresh with rotation, check and revoke. */
public interface AuthSessionService {

    IssuedSession start(User user, SessionClient client, String userAgent);

    /**
     * Rotates the refresh token. Empty when the token is unknown, expired or
     * revoked. A token that was already rotated revokes its session (theft).
     */
    Optional<IssuedSession> refresh(String refreshToken);

    boolean isActive(String sessionId, Long userId);

    void revoke(String sessionId);

    void revokeByRefreshToken(String refreshToken);

    /**
     * Ends the session behind either token. An expired or broken access token is
     * ignored, so sign-out always works.
     */
    void signOut(String accessToken, String refreshToken);

    /** Signs the user out on every device, for example after a password change. */
    void revokeAllForUser(Long userId);
}
