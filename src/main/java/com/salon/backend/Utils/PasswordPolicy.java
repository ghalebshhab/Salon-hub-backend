package com.salon.backend.Utils;

public final class PasswordPolicy {

    public static final String REQUIREMENTS_MESSAGE =
            "Password must contain at least 8 characters, one uppercase letter, one number, and one special character.";

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && !password.isBlank()
                && password.matches(PASSWORD_PATTERN);
    }
}
