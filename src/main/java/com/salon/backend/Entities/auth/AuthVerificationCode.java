package com.salon.backend.Entities.auth;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "auth_verification_codes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_auth_code_user_purpose",
                columnNames = {"user_id", "purpose"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationPurpose purpose;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant resendAvailableAt;

    @Column(nullable = false)
    private int failedAttempts;

    private Instant usedAt;

    @Column(length = 64, unique = true)
    private String resetTokenHash;

    private Instant resetTokenExpiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
