package com.salon.backend.Services.Auth.Verification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Verification.ChangePasswordRequest;
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
import com.salon.backend.Utils.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountVerificationServiceImpl implements AccountVerificationService {

    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(10);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String RECOVERY_RESPONSE =
            "If an eligible account exists for this email, a password reset code has been sent.";

    private final UserRepo userRepo;
    private final AuthVerificationCodeRepo verificationCodeRepo;
    private final AuthEmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.code-secret:${jwt.secret}}")
    private String codeSecret;

    @Value("${app.auth.fixed-code:}")
    private String fixedCode;

    @Override
    @Transactional
    public boolean issueRegistrationVerification(User user) {
        IssuedCode issuedCode = issueCode(user, VerificationPurpose.EMAIL_VERIFICATION);
        boolean delivered = emailService.sendEmailVerificationCode(
                user.getEmail(),
                user.getFirstName(),
                issuedCode.plainCode()
        );
        allowImmediateRetryAfterDeliveryFailure(issuedCode, delivered);
        return delivered;
    }

    @Override
    @Transactional
    public ApiResponse<String> resendEmailVerification(EmailAddressRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        if (email == null) {
            return ApiResponse.error("A valid email address is required.");
        }

        User user = userRepo.findByEmailForUpdate(email).orElse(null);
        if (user == null) {
            return ApiResponse.success(
                    "If an unverified account exists for this email, a verification code has been sent.",
                    null
            );
        }
        if (!Boolean.FALSE.equals(user.getEmailVerified())) {
            return ApiResponse.success("Email address is already verified.", null);
        }

        Optional<AuthVerificationCode> existing = verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        );
        long waitSeconds = remainingCooldownSeconds(existing, Instant.now(clock));
        if (waitSeconds > 0) {
            return ApiResponse.error(
                    "Please wait " + waitSeconds + " seconds before requesting another verification code."
            );
        }

        IssuedCode issuedCode = issueCode(user, VerificationPurpose.EMAIL_VERIFICATION);
        boolean delivered = emailService.sendEmailVerificationCode(
                user.getEmail(),
                user.getFirstName(),
                issuedCode.plainCode()
        );
        if (!delivered) {
            allowImmediateRetryAfterDeliveryFailure(issuedCode, false);
            return ApiResponse.error(
                    "The verification email could not be delivered. Please try again later."
            );
        }
        return ApiResponse.success("A new verification code has been sent.", null);
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyEmail(VerifyCodeRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        String code = normalizeCode(request == null ? null : request.code());
        if (email == null || code == null) {
            return ApiResponse.error("Email address and a 6-digit verification code are required.");
        }

        User user = userRepo.findByEmailForUpdate(email).orElse(null);
        if (user == null) {
            return invalidCodeResponse();
        }
        if (!Boolean.FALSE.equals(user.getEmailVerified())) {
            return ApiResponse.success("Email address is already verified.", null);
        }

        AuthVerificationCode verification = verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION
        ).orElse(null);
        ApiResponse<String> validation = validateCode(verification, code);
        if (validation != null) {
            return validation;
        }

        Instant now = Instant.now(clock);
        verification.setUsedAt(now);
        user.setEmailVerified(true);
        userRepo.save(user);
        verificationCodeRepo.save(verification);
        return ApiResponse.success("Email address verified successfully. You can now sign in.", null);
    }

    @Override
    @Transactional
    public ApiResponse<String> requestPasswordReset(EmailAddressRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        if (email == null) {
            return ApiResponse.error("A valid email address is required.");
        }

        User user = userRepo.findByEmailForUpdate(email).orElse(null);
        if (user == null
                || user.getStatus() != UserStatus.Active
                || Boolean.FALSE.equals(user.getEmailVerified())) {
            return ApiResponse.success(RECOVERY_RESPONSE, null);
        }

        Optional<AuthVerificationCode> existing = verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET
        );
        if (remainingCooldownSeconds(existing, Instant.now(clock)) > 0) {
            return ApiResponse.success(RECOVERY_RESPONSE, null);
        }

        IssuedCode issuedCode = issueCode(user, VerificationPurpose.PASSWORD_RESET);
        boolean delivered = emailService.sendPasswordResetCode(
                user.getEmail(),
                user.getFirstName(),
                issuedCode.plainCode()
        );
        allowImmediateRetryAfterDeliveryFailure(issuedCode, delivered);
        return ApiResponse.success(RECOVERY_RESPONSE, null);
    }

    @Override
    @Transactional
    public ApiResponse<PasswordResetAuthorizationResponse> verifyPasswordResetCode(
            VerifyCodeRequest request
    ) {
        String email = normalizeEmail(request == null ? null : request.email());
        String code = normalizeCode(request == null ? null : request.code());
        if (email == null || code == null) {
            return ApiResponse.error("Email address and a 6-digit verification code are required.");
        }

        User user = userRepo.findByEmailForUpdate(email).orElse(null);
        if (user == null) {
            return ApiResponse.error("The password reset code is invalid or has expired.");
        }

        AuthVerificationCode verification = verificationCodeRepo.findForUpdate(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET
        ).orElse(null);
        ApiResponse<String> validation = validateCode(verification, code);
        if (validation != null) {
            return ApiResponse.error(validation.getMessage());
        }

        Instant now = Instant.now(clock);
        String resetToken = generateResetToken();
        verification.setUsedAt(now);
        verification.setResetTokenHash(hashResetToken(resetToken));
        verification.setResetTokenExpiresAt(now.plus(RESET_TOKEN_LIFETIME));
        verificationCodeRepo.save(verification);

        return ApiResponse.success(
                "Verification code accepted. You can now set a new password.",
                new PasswordResetAuthorizationResponse(
                        resetToken,
                        RESET_TOKEN_LIFETIME.toSeconds()
                )
        );
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        if (request == null || request.resetToken() == null || request.resetToken().isBlank()) {
            return ApiResponse.error("A valid password reset authorization is required.");
        }
        if (!PasswordPolicy.isValid(request.newPassword())) {
            return ApiResponse.error(PasswordPolicy.REQUIREMENTS_MESSAGE);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ApiResponse.error("Password confirmation does not match the new password.");
        }

        String tokenHash = hashResetToken(request.resetToken().trim());
        AuthVerificationCode verification = verificationCodeRepo
                .findPasswordResetTokenForUpdate(
                        tokenHash,
                        VerificationPurpose.PASSWORD_RESET
                )
                .orElse(null);
        Instant now = Instant.now(clock);
        if (verification == null
                || verification.getResetTokenExpiresAt() == null
                || !verification.getResetTokenExpiresAt().isAfter(now)) {
            return ApiResponse.error(
                    "The password reset authorization is invalid or has expired. Please request a new code."
            );
        }

        User user = userRepo.findByIdForUpdate(verification.getUser().getId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.Active) {
            return ApiResponse.error("The password could not be reset for this account.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCredentialsUpdatedAt(now.truncatedTo(ChronoUnit.SECONDS));
        verification.setResetTokenHash(null);
        verification.setResetTokenExpiresAt(null);
        userRepo.save(user);
        verificationCodeRepo.save(verification);
        return ApiResponse.success(
                "Password reset successfully. Please sign in with your new password.",
                null
        );
    }

    @Override
    @Transactional
    public ApiResponse<String> changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {
        if (userId == null) {
            return ApiResponse.error("An authenticated account is required.");
        }
        if (request == null || request.currentPassword() == null
                || request.currentPassword().isBlank()) {
            return ApiResponse.error("Your current password is required.");
        }
        if (!PasswordPolicy.isValid(request.newPassword())) {
            return ApiResponse.error(PasswordPolicy.REQUIREMENTS_MESSAGE);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ApiResponse.error("Password confirmation does not match the new password.");
        }

        User user = userRepo.findByIdForUpdate(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.Active) {
            return ApiResponse.error("The authenticated account is not available.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return ApiResponse.error("The current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCredentialsUpdatedAt(
                Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
        );
        userRepo.save(user);
        return ApiResponse.success(
                "Password changed successfully. Please sign in again on your devices.",
                null
        );
    }

    private IssuedCode issueCode(
            User user,
            VerificationPurpose purpose
    ) {
        Instant now = Instant.now(clock);
        AuthVerificationCode verification = verificationCodeRepo.findForUpdate(user.getId(), purpose)
                .orElseGet(AuthVerificationCode::new);
        String plainCode = generateVerificationCode();

        verification.setUser(user);
        verification.setPurpose(purpose);
        verification.setCodeHash(hashCode(plainCode));
        verification.setExpiresAt(now.plus(CODE_LIFETIME));
        verification.setResendAvailableAt(now.plus(RESEND_COOLDOWN));
        verification.setFailedAttempts(0);
        verification.setUsedAt(null);
        verification.setResetTokenHash(null);
        verification.setResetTokenExpiresAt(null);
        if (verification.getCreatedAt() == null) {
            verification.setCreatedAt(now);
        }
        verificationCodeRepo.save(verification);
        return new IssuedCode(plainCode, verification);
    }

    private String generateVerificationCode() {
        if (fixedCode != null && fixedCode.matches("^\\d{6}$")) {
            return fixedCode;
        }
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private void allowImmediateRetryAfterDeliveryFailure(
            IssuedCode issuedCode,
            boolean delivered
    ) {
        if (delivered) {
            return;
        }
        issuedCode.verification().setResendAvailableAt(Instant.now(clock));
        verificationCodeRepo.save(issuedCode.verification());
    }

    private ApiResponse<String> validateCode(
            AuthVerificationCode verification,
            String plainCode
    ) {
        Instant now = Instant.now(clock);
        if (verification == null || verification.getUsedAt() != null) {
            return invalidCodeResponse();
        }
        if (!verification.getExpiresAt().isAfter(now)) {
            return ApiResponse.error("The verification code has expired. Please request a new code.");
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return ApiResponse.error(
                    "Too many incorrect attempts. Please request a new verification code."
            );
        }
        if (!secureEquals(verification.getCodeHash(), hashCode(plainCode))) {
            verification.setFailedAttempts(verification.getFailedAttempts() + 1);
            verificationCodeRepo.save(verification);
            if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                return ApiResponse.error(
                        "Too many incorrect attempts. Please request a new verification code."
                );
            }
            return invalidCodeResponse();
        }
        return null;
    }

    private ApiResponse<String> invalidCodeResponse() {
        return ApiResponse.error("The verification code is invalid or has expired.");
    }

    private long remainingCooldownSeconds(
            Optional<AuthVerificationCode> verification,
            Instant now
    ) {
        if (verification.isEmpty()
                || verification.get().getResendAvailableAt() == null
                || !verification.get().getResendAvailableAt().isAfter(now)) {
            return 0;
        }
        return Math.max(1, Duration.between(now, verification.get().getResendAvailableAt()).toSeconds());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? normalized : null;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        return normalized.matches("^\\d{6}$") ? normalized : null;
    }

    private String hashCode(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(codeSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not secure the verification code.", exception);
        }
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashResetToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not secure the password reset authorization.", exception);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private record IssuedCode(
            String plainCode,
            AuthVerificationCode verification
    ) {
    }
}
