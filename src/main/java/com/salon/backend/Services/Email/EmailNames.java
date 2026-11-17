package com.salon.backend.Services.Email;

import com.salon.backend.Entities.users.User;

/** Display names used in emails. */
public final class EmailNames {

    private EmailNames() {}

    /** First name for the greeting, else the username. */
    public static String firstName(User user) {
        if (user == null) return null;
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) return user.getFirstName().trim();
        return user.getUserName();
    }

    /** Full name for mentions such as "Sara Ali wants to join". */
    public static String fullName(User user) {
        if (user == null) return "A SalonHub member";
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? user.getUserName() : name;
    }
}
