package com.salon.backend.Services.Email;

/**
 * Published when a business flow wants an email. EmailDispatcher sends it
 * after the transaction commits, so a rolled-back action sends nothing.
 */
public record EmailRequestedEvent(String to, String recipientName, EmailTemplate template) {}
