package com.salon.backend.DTOs.Auth.Verification;

public record ResetPasswordRequest(
        String resetToken,
        String newPassword,
        String confirmPassword
) {
}
