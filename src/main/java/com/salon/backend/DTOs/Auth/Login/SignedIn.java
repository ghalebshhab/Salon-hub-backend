package com.salon.backend.DTOs.Auth.Login;

import com.salon.backend.Services.Auth.Session.IssuedSession;

/**
 * Result of a successful sign-in. The controller writes {@code session} into
 * httpOnly cookies and returns only {@code account} in the response body.
 */
public record SignedIn(LoginResponse account, IssuedSession session) {
}
