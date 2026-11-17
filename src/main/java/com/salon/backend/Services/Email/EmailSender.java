package com.salon.backend.Services.Email;

/** Delivers one rendered email. Throws on failure so the caller can retry. */
public interface EmailSender {
    void send(String to, EmailContent content) throws Exception;
}
