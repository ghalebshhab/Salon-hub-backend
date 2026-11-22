package com.salon.backend.Entities.auth;

/** Which application opened the session. */
public enum SessionClient {
    /** The customer, owner and employee web application (cookie based). */
    WEB,
    /** The admin dashboard (its server keeps the token in its own httpOnly cookie). */
    ADMIN
}
