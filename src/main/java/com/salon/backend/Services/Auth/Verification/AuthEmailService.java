package com.salon.backend.Services.Auth.Verification;

public interface AuthEmailService {

    boolean sendEmailVerificationCode(String recipientEmail, String recipientName, String code);

    boolean sendPasswordResetCode(String recipientEmail, String recipientName, String code);
}
