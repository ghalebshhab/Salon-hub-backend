package com.salon.backend.Services.Auth.Verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
@RequiredArgsConstructor
@Slf4j
public class AuthEmailServiceImpl implements AuthEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    @Override
    public boolean sendEmailVerificationCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        return send(
                recipientEmail,
                "Verify your Salon Hup email",
                greeting(recipientName)
                        + "\n\nYour Salon Hup verification code is: " + code
                        + "\n\nThis code expires in 10 minutes."
                        + " If you did not create this account, you can ignore this email."
        );
    }

    @Override
    public boolean sendPasswordResetCode(
            String recipientEmail,
            String recipientName,
            String code
    ) {
        return send(
                recipientEmail,
                "Reset your Salon Hup password",
                greeting(recipientName)
                        + "\n\nYour Salon Hup password reset code is: " + code
                        + "\n\nThis code expires in 10 minutes."
                        + " If you did not request a password reset, you can ignore this email."
        );
    }

    private boolean send(String recipientEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress.trim());
            }
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException | IllegalArgumentException exception) {
            log.warn("Authentication email delivery failed: {}",
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    private String greeting(String recipientName) {
        if (recipientName == null || recipientName.isBlank()) {
            return "Hello,";
        }
        return "Hello " + recipientName.trim() + ",";
    }
}
