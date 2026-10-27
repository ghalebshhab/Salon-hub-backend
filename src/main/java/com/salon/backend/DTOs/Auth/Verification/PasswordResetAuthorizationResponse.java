package com.salon.backend.DTOs.Auth.Verification;

public record PasswordResetAuthorizationResponse(
        String resetToken,
        long expiresInSeconds
) {
}
