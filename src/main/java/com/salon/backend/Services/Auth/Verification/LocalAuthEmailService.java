package com.salon.backend.Services.Auth.Verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Keeps local authentication flows self-contained and prevents accidental
 * SMTP delivery while developing. Codes are visible only in the local backend
 * process log so email verification and password recovery can be tested.
 */
@Service
@Profile("local")
@Slf4j
public class LocalAuthEmailService implements AuthEmailService {

    @Override
    public boolean sendEmailVerificationCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        log.info(
                "LOCAL_AUTH_CODE purpose=email-verification email={} code={}",
                recipientEmail,
                code
        );
        return true;
    }

    @Override
    public boolean sendPasswordResetCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        log.info(
                "LOCAL_AUTH_CODE purpose=password-reset email={} code={}",
                recipientEmail,
                code
        );
        return true;
    }
}
