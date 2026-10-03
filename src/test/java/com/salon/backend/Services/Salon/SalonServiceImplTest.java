package com.salon.backend.Services.Salon;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceImplTest {

    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @InjectMocks private SalonServiceImpl service;

    @Test
    void activeSalonsAreQueriedByStatusAndMappedWithoutEntityGraphs() {
        User owner = new User();
        owner.setId(4L);
        owner.setUserName("owner");
        owner.setPassword("must-not-be-returned");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setName("Studio");
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);

        when(salonRepo.findByStatus(SalonStatus.Accepted)).thenReturn(List.of(salon));

        ApiResponse<List<SalonResponse>> response = service.getActiveSalons();

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(salon.getId(), response.getData().get(0).id());
        assertEquals(owner.getId(), response.getData().get(0).ownerId());
        verify(salonRepo).findByStatus(SalonStatus.Accepted);
        verify(salonRepo, never()).findAll();
    }

    @Test
    void rejectedSalonCanBeResubmittedForTheThirdAndFinalTime() {
        User owner = new User();
        owner.setId(4L);
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Rejected);
        salon.setRejectionReason("Fix the salon details");
        salon.setResubmissionCount(2);

        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<SalonResponse> response =
                service.resubmitRejectedSalon(salon.getId(), owner.getId());

        assertTrue(response.isSuccess());
        assertEquals(SalonStatus.Requested, salon.getStatus());
        assertEquals(3, salon.getResubmissionCount());
        assertEquals(0, response.getData().remainingResubmissions());
        assertNull(salon.getRejectionReason());
        verify(salonRepo).save(salon);
    }

    @Test
    void rejectedSalonCannotExceedThreeResubmissions() {
        User owner = new User();
        owner.setId(4L);
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Rejected);
        salon.setResubmissionCount(3);

        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<SalonResponse> response =
                service.resubmitRejectedSalon(salon.getId(), owner.getId());

        assertFalse(response.isSuccess());
        assertEquals(SalonStatus.Rejected, salon.getStatus());
        verify(salonRepo, never()).save(any());
    }

    @Test
    void allSalonsAreNotQueriedWhenAdminAuthorizationFails() {
        when(adminAuthorizationService.authorize(6L))
                .thenReturn(ApiResponse.error("You are not Admin , Access Denied"));

        ApiResponse<List<SalonResponse>> response = service.getAllSalons(6L);

        assertFalse(response.isSuccess());
        assertEquals("You are not Admin , Access Denied", response.getMessage());
        verifyNoInteractions(salonRepo);
    }
}
