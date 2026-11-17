package com.salon.backend.Services.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Renders and delivers emails. Queued emails run on the "emailExecutor"
 * thread pool only after the business transaction commits, so a request
 * never waits for the mail server and a rolled-back action sends nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatcher {

    private static final long FIRST_RETRY_DELAY_MS = 2_000;

    private final EmailRenderer renderer;
    private final EmailSender sender;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailRequested(EmailRequestedEvent event) {
        deliver(event);
    }

    /** Tries up to the priority's attempt count with growing waits. */
    public boolean deliver(EmailRequestedEvent event) {
        EmailTemplate template = event.template();
        EmailContent content = renderer.render(template, event.recipientName());
        int attempts = Math.max(1, template.priority().attempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                sender.send(event.to(), content);
                return true;
            } catch (Exception exception) {
                log.warn("Email \"{}\" to {} failed (attempt {}/{}): {}",
                        template.subject(), mask(event.to()), attempt, attempts, exception.toString());
                if (attempt < attempts && !sleep(FIRST_RETRY_DELAY_MS * attempt)) break;
            }
        }
        log.error("Email \"{}\" to {} was not delivered.", template.subject(), mask(event.to()));
        return false;
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Keeps full addresses out of the logs: "gh***@icloud.com". */
    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
