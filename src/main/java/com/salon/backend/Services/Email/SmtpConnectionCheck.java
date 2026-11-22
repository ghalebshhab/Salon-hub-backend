package com.salon.backend.Services.Email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Signs in to the mail server once at start-up and logs the result, so a
 * wrong App Password or a blocked SMTP port shows in the log at once and
 * not first when a user waits for a verification code. It sends no email.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "smtp", matchIfMissing = true)
public class SmtpConnectionCheck {

    private final JavaMailSender mailSender;

    public SmtpConnectionCheck(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("emailExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (!(mailSender instanceof JavaMailSenderImpl smtp)) {
            return;
        }
        try {
            smtp.testConnection();
            log.info("Email: signed in to {}:{} as {}", smtp.getHost(), smtp.getPort(), smtp.getUsername());
        } catch (Exception error) {
            log.error("Email: cannot sign in to {}:{} as {}. Check MAIL_PASSWORD (a Gmail App Password) "
                            + "and that the host allows this SMTP port. Cause: {}",
                    smtp.getHost(), smtp.getPort(), smtp.getUsername(), error.toString());
        }
    }
}
