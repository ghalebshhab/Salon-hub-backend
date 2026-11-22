package com.salon.backend.Services.Auth.Session;

import com.salon.backend.Entities.auth.AuthSession;
import com.salon.backend.Entities.auth.SessionClient;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Auth.AuthSessionRepo;
import com.salon.backend.Services.Auth.Security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthSessionServiceImpl implements AuthSessionService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int USER_AGENT_MAX = 255;
    /** Two tabs can refresh at the same moment; the slower one must not count as theft. */
    private static final Duration ROTATION_GRACE = Duration.ofSeconds(30);
    /** Ended sessions are kept a short time for support questions, then deleted. */
    private static final Duration ENDED_RETENTION = Duration.ofDays(30);

    private final AuthSessionRepo sessionRepo;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Duration adminTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthSessionServiceImpl(
            AuthSessionRepo sessionRepo,
            JwtService jwtService,
            Clock clock,
            @Value("${salon.auth.access-ttl:PT15M}") Duration accessTtl,
            @Value("${salon.auth.refresh-ttl:P14D}") Duration refreshTtl,
            @Value("${salon.auth.admin-ttl:PT8H}") Duration adminTtl
    ) {
        this.sessionRepo = sessionRepo;
        this.jwtService = jwtService;
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.adminTtl = adminTtl;
    }

    @Override
    @Transactional
    public IssuedSession start(User user, SessionClient client, String userAgent) {
        Instant now = Instant.now(clock);
        boolean admin = client == SessionClient.ADMIN;
        Duration sessionTtl = admin ? adminTtl : refreshTtl;

        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setClient(client);
        session.setUserAgent(trimUserAgent(userAgent));
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plus(sessionTtl));

        // The admin dashboard has no refresh step: one token for the whole session.
        String refreshToken = admin ? null : newRefreshToken();
        session.setRefreshTokenHash(refreshToken == null ? null : hash(refreshToken));
        sessionRepo.save(session);

        Duration tokenTtl = admin ? adminTtl : accessTtl;
        return new IssuedSession(
                session.getId(),
                jwtService.generateAccessToken(user, session.getId(), tokenTtl),
                tokenTtl,
                refreshToken,
                admin ? Duration.ZERO : refreshTtl
        );
    }

    @Override
    @Transactional
    public Optional<IssuedSession> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        String presentedHash = hash(refreshToken);

        Optional<AuthSession> current = sessionRepo.findByRefreshTokenHashForUpdate(presentedHash);
        if (current.isEmpty()) {
            sessionRepo.findByPreviousRefreshTokenHash(presentedHash)
                    .filter(old -> old.getRevokedAt() == null)
                    .filter(old -> old.getLastUsedAt().plus(ROTATION_GRACE).isBefore(now))
                    .ifPresent(old -> {
                        log.warn("Refresh token reuse detected; revoking session {}", old.getId());
                        old.setRevokedAt(now);
                    });
            return Optional.empty();
        }

        AuthSession session = current.get();
        User user = session.getUser();
        if (!session.isActiveAt(now) || user.getStatus() != UserStatus.Active) {
            return Optional.empty();
        }

        String nextRefreshToken = newRefreshToken();
        session.setPreviousRefreshTokenHash(presentedHash);
        session.setRefreshTokenHash(hash(nextRefreshToken));
        session.setLastUsedAt(now);
        // Sliding window: an active device stays signed in, an idle one expires.
        session.setExpiresAt(now.plus(refreshTtl));

        return Optional.of(new IssuedSession(
                session.getId(),
                jwtService.generateAccessToken(user, session.getId(), accessTtl),
                accessTtl,
                nextRefreshToken,
                refreshTtl
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String sessionId, Long userId) {
        return sessionId != null && userId != null
                && sessionRepo.isActive(sessionId, userId, Instant.now(clock));
    }

    @Override
    @Transactional
    public void revoke(String sessionId) {
        if (sessionId != null) {
            sessionRepo.revoke(sessionId, Instant.now(clock));
        }
    }

    @Override
    @Transactional
    public void revokeByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessionRepo.findByRefreshTokenHashForUpdate(hash(refreshToken))
                .ifPresent(session -> session.setRevokedAt(Instant.now(clock)));
    }

    @Override
    @Transactional
    public void signOut(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                revoke(jwtService.extractSessionId(accessToken));
            } catch (RuntimeException ignored) {
                // Expired or invalid token: the refresh token below still ends the session.
            }
        }
        revokeByRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId != null) {
            sessionRepo.revokeAllForUser(userId, Instant.now(clock));
        }
    }

    /** Daily at 04:15 (server time) remove sessions that ended more than 30 days ago. */
    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void deleteEndedSessions() {
        int removed = sessionRepo.deleteEndedBefore(Instant.now(clock).minus(ENDED_RETENTION));
        if (removed > 0) {
            log.info("Removed {} ended sign-in sessions", removed);
        }
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static String trimUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= USER_AGENT_MAX ? userAgent : userAgent.substring(0, USER_AGENT_MAX);
    }
}
