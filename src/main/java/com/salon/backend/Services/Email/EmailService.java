package com.salon.backend.Services.Email;

/**
 * The one entry point for email in SalonHub.
 *
 * <p>{@link #queue} is for event emails: they go out in the background after
 * the current transaction commits, with retries by priority.
 * {@link #sendNow} is for security codes the user waits for; it returns
 * whether delivery worked.
 */
public interface EmailService {

    void queue(String to, String recipientName, EmailTemplate template);

    boolean sendNow(String to, String recipientName, EmailTemplate template);
}
