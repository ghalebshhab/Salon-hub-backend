package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Salon.SalonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SalonRepo salonRepo;
    @Mock private SalonService salonService;
    @InjectMocks private DashboardServiceImpl service;

    @Test
    void rejectionStoresTrimmedReasonForTheOwner() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);

        User owner = new User();
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(9L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Requested);

        SalonResponse mappedResponse = new SalonResponse(
                salon.getId(), null, null, null, null, null,
                owner.getId(), null, null, null, null, null,
                null, null, null, SalonStatus.Rejected, null,
                "Missing license", 0, 3
        );

        when(userRepo.findByemail(admin.getEmail())).thenReturn(admin);
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));
        when(salonService.getSalonByOwnerEmail(owner.getEmail()))
                .thenReturn(ApiResponse.success("Salon Fetched Successfully", mappedResponse));

        ApiResponse<SalonResponse> response = service.rejectSalonRequest(
                salon.getId(),
                admin.getEmail(),
                "  Missing license  "
        );

        assertTrue(response.isSuccess());
        assertEquals(SalonStatus.Rejected, salon.getStatus());
        assertEquals("Missing license", salon.getRejectionReason());
        verify(salonRepo).save(salon);
    }
}
