package com.salon.backend.Services.Email;

/** A rendered email: subject, HTML body and plain-text alternative. */
public record EmailContent(String subject, String html, String text) {}
