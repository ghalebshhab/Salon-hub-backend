package com.salon.backend.Entities.auth;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One signed-in device. The access token carries this row's id ("sid"), so
 * revoking the row signs the device out at once, also after a restart and
 * across server instances. Only a SHA-256 hash of the refresh token is kept.
 */
@Entity
@Table(
        name = "auth_sessions",
        indexes = {
                @Index(name = "idx_auth_session_user", columnList = "user_id"),
                @Index(name = "idx_auth_session_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AuthSession {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionClient client;

    @Column(name = "refresh_token_hash", length = 64, unique = true)
    private String refreshTokenHash;

    /** The refresh token used before the last rotation; seeing it again means theft. */
    @Column(name = "previous_refresh_token_hash", length = 64)
    private String previousRefreshTokenHash;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    private Long version;

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
