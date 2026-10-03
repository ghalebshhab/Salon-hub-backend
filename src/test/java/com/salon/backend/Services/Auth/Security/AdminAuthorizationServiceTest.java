package com.salon.backend.Services.Auth.Security;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock private UserRepo userRepo;
    @InjectMocks private AdminAuthorizationService service;

    @Test
    void activeAdminIsAuthorizedByImmutableId() {
        User admin = user(7L, UserRole.ADMIN, UserStatus.Active);
        when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));

        ApiResponse<User> response = service.authorize(admin.getId());

        assertTrue(response.isSuccess());
        assertSame(admin, response.getData());
    }

    @Test
    void nonAdminIsRejected() {
        User user = user(8L, UserRole.USER, UserStatus.Active);
        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));

        ApiResponse<User> response = service.authorize(user.getId());

        assertFalse(response.isSuccess());
    }

    @Test
    void blockedAdminIsRejected() {
        User admin = user(9L, UserRole.ADMIN, UserStatus.Blocked);
        when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));

        ApiResponse<User> response = service.authorize(admin.getId());

        assertFalse(response.isSuccess());
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
