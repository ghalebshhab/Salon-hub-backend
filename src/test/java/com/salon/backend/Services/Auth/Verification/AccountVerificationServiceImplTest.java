package com.salon.backend.Services.Auth.Verification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Verification.EmailAddressRequest;
import com.salon.backend.DTOs.Auth.Verification.PasswordResetAuthorizationResponse;
import com.salon.backend.DTOs.Auth.Verification.ResetPasswordRequest;
import com.salon.backend.DTOs.Auth.Verification.VerifyCodeRequest;
import com.salon.backend.Entities.auth.AuthVerificationCode;
import com.salon.backend.Entities.auth.VerificationPurpose;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Auth.AuthVerificationCodeRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    @Mock private UserRepo userRepo;
    @Mock private AuthVerificationCodeRepo verificationCodeRepo;
    @Mock private AuthEmailService emailService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    private AccountVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountVerificationServiceImpl(
                userRepo,
                verificationCodeRepo,
                emailService,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(
                service,
                "codeSecret",
                "a-test-code-secret-with-at-least-32-characters"
        );
    }

    @Test
    void registrationStoresOnlyAHashAndTheDeliveredCodeCanVerifyTheEmail() {
        User user = user(7L, false);
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.empty());
        when(emailService.sendEmailVerificationCode(
                eq(user.getEmail()),
                eq(user.getFirstName()),
                anyString()
        )).thenReturn(true);

        assertTrue(service.issueRegistrationVerification(user));

        ArgumentCaptor<String> deliveredCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailVerificationCode(
                eq(user.getEmail()),
                eq(user.getFirstName()),
                deliveredCode.capture()
        );
        assertTrue(deliveredCode.getValue().matches("\\d{6}"));

        ArgumentCaptor<AuthVerificationCode> storedCode =
                ArgumentCaptor.forClass(AuthVerificationCode.class);
        verify(verificationCodeRepo).save(storedCode.capture());
        AuthVerificationCode verification = storedCode.getValue();
        assertNotEquals(deliveredCode.getValue(), verification.getCodeHash());
        assertEquals(NOW.plusSeconds(600), verification.getExpiresAt());
        assertEquals(NOW.plusSeconds(60), verification.getResendAvailableAt());

        when(userRepo.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.of(verification));

        ApiResponse<String> response = service.verifyEmail(
                new VerifyCodeRequest(user.getEmail(), deliveredCode.getValue())
        );

        assertTrue(response.isSuccess());
        assertTrue(user.getEmailVerified());
        assertEquals(NOW, verification.getUsedAt());
    }

    @Test
    void resendEnforcesTheSixtySecondCooldown() {
        User user = user(8L, false);
        AuthVerificationCode verification = new AuthVerificationCode();
        verification.setResendAvailableAt(NOW.plusSeconds(30));
        when(userRepo.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.of(verification));

        ApiResponse<String> response = service.resendEmailVerification(
                new EmailAddressRequest(user.getEmail())
        );

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("30 seconds"));
        verify(emailService, never()).sendEmailVerificationCode(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void fifthIncorrectCodeAttemptLocksTheCodeUntilResend() {
        User user = user(10L, false);
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.empty());
        when(emailService.sendEmailVerificationCode(
                eq(user.getEmail()),
                eq(user.getFirstName()),
                anyString()
        )).thenReturn(true);
        service.issueRegistrationVerification(user);

        ArgumentCaptor<AuthVerificationCode> storedCode =
                ArgumentCaptor.forClass(AuthVerificationCode.class);
        verify(verificationCodeRepo).save(storedCode.capture());
        AuthVerificationCode verification = storedCode.getValue();
        when(userRepo.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.of(verification));

        ApiResponse<String> response = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            response = service.verifyEmail(
                    new VerifyCodeRequest(user.getEmail(), "999999")
            );
        }

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(
                "Too many incorrect attempts. Please request a new verification code.",
                response.getMessage()
        );
        assertEquals(5, verification.getFailedAttempts());
        assertFalse(user.getEmailVerified());
    }

    @Test
    void unknownPasswordRecoveryEmailReturnsTheSameGenericSuccess() {
        when(userRepo.findByEmailForUpdate("missing@example.com"))
                .thenReturn(Optional.empty());

        ApiResponse<String> response = service.requestPasswordReset(
                new EmailAddressRequest("missing@example.com")
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().startsWith("If an eligible account exists"));
        verify(emailService, never()).sendPasswordResetCode(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void verifiedResetCodeProducesSingleUseTokenAndChangesPassword() {
        User user = user(9L, true);
        when(userRepo.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET
        )).thenReturn(Optional.empty());
        when(emailService.sendPasswordResetCode(
                eq(user.getEmail()),
                eq(user.getFirstName()),
                anyString()
        )).thenReturn(true);

        service.requestPasswordReset(new EmailAddressRequest(user.getEmail()));

        ArgumentCaptor<String> deliveredCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetCode(
                eq(user.getEmail()),
                eq(user.getFirstName()),
                deliveredCode.capture()
        );
        ArgumentCaptor<AuthVerificationCode> storedCode =
                ArgumentCaptor.forClass(AuthVerificationCode.class);
        verify(verificationCodeRepo).save(storedCode.capture());
        AuthVerificationCode verification = storedCode.getValue();

        when(verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET
        )).thenReturn(Optional.of(verification));
        ApiResponse<PasswordResetAuthorizationResponse> authorization =
                service.verifyPasswordResetCode(
                        new VerifyCodeRequest(user.getEmail(), deliveredCode.getValue())
                );

        assertTrue(authorization.isSuccess());
        assertNotNull(authorization.getData().resetToken());
        assertEquals(600, authorization.getData().expiresInSeconds());
        assertNotNull(verification.getResetTokenHash());
        assertNotEquals(
                authorization.getData().resetToken(),
                verification.getResetTokenHash()
        );

        when(verificationCodeRepo.findPasswordResetTokenForUpdate(
                anyString(),
                eq(VerificationPurpose.PASSWORD_RESET)
        )).thenReturn(Optional.of(verification));
        when(userRepo.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword2!"))
                .thenReturn("new-password-hash");

        ApiResponse<String> reset = service.resetPassword(
                new ResetPasswordRequest(
                        authorization.getData().resetToken(),
                        "NewPassword2!",
                        "NewPassword2!"
                )
        );

        assertTrue(reset.isSuccess());
        assertEquals("new-password-hash", user.getPassword());
        assertEquals(NOW, user.getCredentialsUpdatedAt());
        assertNull(verification.getResetTokenHash());
        assertNull(verification.getResetTokenExpiresAt());
    }

    private User user(long id, boolean emailVerified) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Maya");
        user.setEmail("maya@example.com");
        user.setEmailVerified(emailVerified);
        user.setStatus(UserStatus.Active);
        user.setPassword("old-password-hash");
        return user;
    }
}
