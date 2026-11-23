package com.salon.backend.Services.Email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Checks the Brevo API key once at start-up and logs the result, so a wrong
 * key shows in the log at once. It sends no email.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "brevo")
public class BrevoConnectionCheck {

    private static final String ACCOUNT_URL = "https://api.brevo.com/v3/account";

    private final String apiKey;
    private final String fromAddress;

    public BrevoConnectionCheck(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String fromAddress) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
    }

    @Async("emailExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (apiKey.isEmpty() || fromAddress.isEmpty()) {
            log.error("Email: Brevo is selected but BREVO_API_KEY or MAIL_FROM is empty. No email will be sent.");
            return;
        }
        try {
            RestClient.create().get()
                    .uri(ACCOUNT_URL)
                    .header("api-key", apiKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email: Brevo API key accepted; sending as {}", fromAddress);
        } catch (Exception error) {
            log.error("Email: Brevo did not accept the API key. Check BREVO_API_KEY. Cause: {}", error.toString());
        }
    }
}
