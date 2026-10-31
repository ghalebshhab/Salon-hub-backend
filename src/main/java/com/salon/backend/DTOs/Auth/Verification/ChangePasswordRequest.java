package com.salon.backend.DTOs.Auth.Verification;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
