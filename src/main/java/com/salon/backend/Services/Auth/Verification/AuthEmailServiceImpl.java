package com.salon.backend.Services.Auth.Verification;

import com.salon.backend.Services.Email.EmailService;
import com.salon.backend.Services.Email.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Sends the security codes through the shared email service. The user waits
 * for these codes, so they go out now and the caller learns if they failed.
 */
@Service
@Profile("!local")
@RequiredArgsConstructor
public class AuthEmailServiceImpl implements AuthEmailService {

    private final EmailService emailService;
    private final EmailTemplates templates;

    @Override
    public boolean sendEmailVerificationCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        return emailService.sendNow(recipientEmail, recipientName, templates.verificationCode(code));
    }

    @Override
    public boolean sendPasswordResetCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        return emailService.sendNow(recipientEmail, recipientName, templates.passwordResetCode(code));
    }
}
