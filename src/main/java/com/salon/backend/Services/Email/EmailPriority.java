package com.salon.backend.Services.Email;

/**
 * How important an email is. It sets the retry count and whether the email
 * is sent at all. LOW events stay in the in-app notification inbox only.
 */
public enum EmailPriority {
    /** Security: codes, password changes. Sent at once, 3 attempts. */
    CRITICAL(3),
    /** Decisions the user must know: approvals, bookings, employment. */
    HIGH(3),
    /** Helpful information: welcome, login alert, reminders, receipts. */
    NORMAL(2),
    /** Live or minor updates. In-app only, no email. */
    LOW(0);

    private final int attempts;

    EmailPriority(int attempts) {
        this.attempts = attempts;
    }

    public int attempts() {
        return attempts;
    }

    public boolean sendsEmail() {
        return attempts > 0;
    }
}
