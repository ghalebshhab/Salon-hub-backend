package com.salon.backend.Services.Email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local development: writes the email to the log instead of sending it
 * (app.mail.delivery=log). Set it to smtp to receive real emails locally.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "log")
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, EmailContent content) {
        log.info("EMAIL (log only) to={} subject=\"{}\"\n{}", to, content.subject(), content.text());
    }
}
