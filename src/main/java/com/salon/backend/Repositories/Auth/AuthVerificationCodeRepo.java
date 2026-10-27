package com.salon.backend.Repositories.Auth;

import com.salon.backend.Entities.auth.AuthVerificationCode;
import com.salon.backend.Entities.auth.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthVerificationCodeRepo extends JpaRepository<AuthVerificationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT code
            FROM AuthVerificationCode code
            WHERE code.user.id = :userId AND code.purpose = :purpose
            """)
    Optional<AuthVerificationCode> findForUpdate(
            @Param("userId") Long userId,
            @Param("purpose") VerificationPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT code
            FROM AuthVerificationCode code
            WHERE code.resetTokenHash = :resetTokenHash
              AND code.purpose = :purpose
            """)
    Optional<AuthVerificationCode> findPasswordResetTokenForUpdate(
            @Param("resetTokenHash") String resetTokenHash,
            @Param("purpose") VerificationPurpose purpose
    );
}
