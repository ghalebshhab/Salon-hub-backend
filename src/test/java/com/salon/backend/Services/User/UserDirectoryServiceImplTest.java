package com.salon.backend.Services.User;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @InjectMocks private UserDirectoryServiceImpl service;

    @Test
    void activeUsersAreLoadedByStatusAndMappedToSafeDtos() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Maya");
        user.setLastName("Ali");
        user.setEmail("maya@example.com");
        user.setUserName("maya");
        user.setPhoneNumber("0790000000");
        user.setPassword("must-not-be-returned");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.Active);

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Active);
        when(adminAuthorizationService.authorize(admin.getId()))
                .thenReturn(ApiResponse.success("Admin authorized", admin));
        when(userRepo.findByStatus(UserStatus.Active)).thenReturn(List.of(user));

        ApiResponse<List<UserSummaryResponse>> response = service.getActiveUsers(admin.getId());

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(user.getId(), response.getData().get(0).id());
        assertEquals(user.getEmail(), response.getData().get(0).email());
        verify(userRepo).findByStatus(UserStatus.Active);
        verify(userRepo, never()).findAll();
    }

    @Test
    void directoryDoesNotQueryUsersWhenAdminAuthorizationFails() {
        when(adminAuthorizationService.authorize(4L))
                .thenReturn(ApiResponse.error("You are not Admin , Access Denied"));

        ApiResponse<List<UserSummaryResponse>> response = service.getUsers(4L);

        assertFalse(response.isSuccess());
        assertEquals("You are not Admin , Access Denied", response.getMessage());
        verifyNoInteractions(userRepo);
    }
}
