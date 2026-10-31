package com.salon.backend.Services.User.Profile;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
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

    @Mock
    private SalonRepo salonRepo;

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
        verify(salonRepo).findByOwnerId(user.getId());
        verifyNoMoreInteractions(userRepo, salonRepo);
    }

    @Test
    void profileSeparatesOwnedSalonFromEmploymentSalon() {
        User user = new User();
        user.setId(21L);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.Active);

        Salon employmentSalon = new Salon();
        employmentSalon.setId(4L);
        employmentSalon.setName("Employer salon");
        user.setSalon(employmentSalon);

        Salon ownedSalon = new Salon();
        ownedSalon.setId(9L);
        ownedSalon.setName("Owned salon");
        ownedSalon.setStatus(SalonStatus.Requested);

        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));
        when(salonRepo.findByOwnerId(user.getId())).thenReturn(ownedSalon);

        UserProfileResponse profile = service.getProfile(user.getId()).getData();

        assertEquals(4L, profile.salonId());
        assertEquals(9L, profile.ownedSalonId());
        assertEquals(SalonStatus.Requested, profile.ownedSalonStatus());
    }
}
