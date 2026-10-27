package com.salon.backend.Services.Auth;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Rigester.RigesterRequest;
import com.salon.backend.DTOs.Auth.Rigester.RigesterResponse;
import com.salon.backend.DTOs.Auth.Login.LoginRequest;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Verification.AccountVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepo userRepo;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AccountVerificationService accountVerificationService;
    @InjectMocks private AuthServiceImpl service;

    @Test
    void registrationCreatesDefaultProfileFromRegistrationData() {
        RigesterRequest request = new RigesterRequest(
                "  Maya  ",
                "  Ahmad  ",
                "0791234567",
                "Maya@Gmail.com",
                "Password1!",
                "Maya.Ahmad"
        );

        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(accountVerificationService.issueRegistrationVerification(any(User.class)))
                .thenReturn(true);
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(25L);
            return savedUser;
        });

        ApiResponse<RigesterResponse> response = service.Rigester(request);

        assertTrue(response.isSuccess());
        assertEquals(25L, response.getData().getId());
        assertEquals("Maya", response.getData().getFirstName());
        assertEquals("Ahmad", response.getData().getLastName());
        assertEquals("maya.ahmad", response.getData().getUserName());
        assertEquals("maya@gmail.com", response.getData().getEmail());
        assertEquals("0791234567", response.getData().getPhoneNumber());
        assertTrue(response.getData().isEmailVerificationRequired());
        assertEquals(600, response.getData().getVerificationCodeExpiresInSeconds());
        assertEquals(60, response.getData().getResendAvailableInSeconds());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Maya", savedUser.getFirstName());
        assertEquals("Ahmad", savedUser.getLastName());
        assertEquals("maya.ahmad", savedUser.getUserName());
        assertEquals("maya@gmail.com", savedUser.getEmail());
        assertEquals("0791234567", savedUser.getPhoneNumber());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals(UserStatus.Active, savedUser.getStatus());
        assertFalse(savedUser.getEmailVerified());
        assertFalse(savedUser.isCareerProfileEnabled());
        assertNotNull(savedUser.getCareerSkills());
        assertTrue(savedUser.getCareerSkills().isEmpty());
        assertNotNull(savedUser.getPreferredWorkLocations());
        assertTrue(savedUser.getPreferredWorkLocations().isEmpty());
        assertNull(savedUser.getDateOfBirth());
        assertNull(savedUser.getYearsOfExperience());
        assertNull(savedUser.getResumeUrl());
        assertNull(savedUser.getPortfolioUrl());
        assertNull(savedUser.getProfessionalBio());
        verify(accountVerificationService).issueRegistrationVerification(savedUser);
    }

    @Test
    void loginRequiresEmailVerificationAfterValidCredentials() {
        User user = new User();
        user.setEmail("maya@gmail.com");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.Active);
        user.setEmailVerified(false);
        when(userRepo.findByemail("maya@gmail.com")).thenReturn(user);
        when(passwordEncoder.matches("Password1!", "encoded-password")).thenReturn(true);

        ApiResponse<?> response = service.Login(
                new LoginRequest("maya@gmail.com", "Password1!")
        );

        assertFalse(response.isSuccess());
        assertEquals(
                "Email verification is required before signing in.",
                response.getMessage()
        );
        verify(jwtService, never()).generateToken(any(User.class));
    }
}
