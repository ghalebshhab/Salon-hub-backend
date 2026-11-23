package com.salon.backend.Services.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends email through the Brevo HTTPS API (app.mail.delivery=brevo).
 * Use it where the host blocks outgoing SMTP ports, for example Render's
 * free plan. Brevo does not take inline (cid:) images, so the logo is
 * loaded from a public address instead.
 */
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "brevo")
public class BrevoEmailSender implements EmailSender {

    static final String SEND_URL = "https://api.brevo.com/v3/smtp/email";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String fromName;
    private final String logoUrl;

    @Autowired
    public BrevoEmailSender(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${app.mail.from-name:SalonHub}") String fromName,
            @Value("${app.mail.logo-url:}") String logoUrl) {
        this(RestClient.builder().requestFactory(timeoutFactory()), apiKey, fromAddress, fromName, logoUrl);
    }

    BrevoEmailSender(RestClient.Builder clientBuilder, String apiKey, String fromAddress,
                     String fromName, String logoUrl) {
        this.restClient = clientBuilder.build();
        this.apiKey = trim(apiKey);
        this.fromAddress = trim(fromAddress);
        this.fromName = trim(fromName).isEmpty() ? "SalonHub" : trim(fromName);
        this.logoUrl = trim(logoUrl);
    }

    @Override
    public void send(String to, EmailContent content) {
        if (apiKey.isEmpty() || fromAddress.isEmpty()) {
            throw new IllegalStateException("Brevo email is not configured. Set BREVO_API_KEY and MAIL_FROM.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("name", fromName, "email", fromAddress));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", content.subject());
        body.put("htmlContent", withHostedLogo(content.html()));
        body.put("textContent", content.text());

        // A 4xx or 5xx answer throws, so the dispatcher can retry and log it.
        restClient.post()
                .uri(SEND_URL)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** Points the cid: logo at the public logo, or removes it when no address is set. */
    String withHostedLogo(String html) {
        String inline = "cid:" + EmailRenderer.LOGO_CID;
        if (html == null || !html.contains(inline)) {
            return html;
        }
        if (!logoUrl.isEmpty()) {
            return html.replace(inline, logoUrl.replace("\"", "%22"));
        }
        return html.replaceAll("<img src=\"" + inline + "\"[^>]*>", "");
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
