package com.salon.backend.Repositories.Auth;

import com.salon.backend.Entities.auth.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepo extends JpaRepository<AuthSession, String> {

    /** Locks the row so two parallel refresh calls cannot both rotate the same token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.refreshTokenHash = :hash")
    Optional<AuthSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    Optional<AuthSession> findByPreviousRefreshTokenHash(String hash);

    @Query("""
            select count(s) > 0 from AuthSession s
            where s.id = :id and s.user.id = :userId
              and s.revokedAt is null and s.expiresAt > :now
            """)
    boolean isActive(@Param("id") String id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now where s.id = :id and s.revokedAt is null")
    int revoke(@Param("id") String id, @Param("now") Instant now);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt < :before or s.revokedAt < :before")
    int deleteEndedBefore(@Param("before") Instant before);
}
