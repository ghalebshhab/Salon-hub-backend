package com.salon.backend.Services.User.Profile;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserProfileServiceImpl service;

    @Test
    void profileUsesImmutableAuthenticatedUserId() {
        User user = new User();
        user.setId(14L);
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.Active);

        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));

        ApiResponse<UserProfileResponse> response = service.getProfile(user.getId());

        assertTrue(response.isSuccess());
        assertEquals(user.getId(), response.getData().id());
        assertEquals(user.getEmail(), response.getData().email());
        verify(userRepo).findById(user.getId());
        verifyNoMoreInteractions(userRepo);
    }
}
