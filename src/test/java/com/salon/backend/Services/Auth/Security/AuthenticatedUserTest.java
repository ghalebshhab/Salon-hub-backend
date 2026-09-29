package com.salon.backend.Services.Auth.Security;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

    @Test
    void onlyActiveUsersAreEnabledForAuthentication() {
        User user = new User();
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.Active);

        assertTrue(AuthenticatedUser.from(user).isEnabled());

        user.setStatus(UserStatus.Blocked);

        assertFalse(AuthenticatedUser.from(user).isEnabled());
    }
}
