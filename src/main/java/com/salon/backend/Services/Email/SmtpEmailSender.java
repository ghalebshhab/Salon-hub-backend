package com.salon.backend.Services.Email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Sends HTML + plain-text email over SMTP (app.mail.delivery=smtp, the default). */
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String fromAddress,
            @Value("${app.mail.from-name:SalonHub}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.fromName = fromName;
    }

    @Override
    public void send(String to, EmailContent content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        if (!fromAddress.isBlank()) helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(content.subject());
        helper.setText(content.text(), content.html());
        mailSender.send(message);
    }
}
