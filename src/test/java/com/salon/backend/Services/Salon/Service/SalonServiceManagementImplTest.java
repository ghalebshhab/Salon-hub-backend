package com.salon.backend.Services.Salon.Service;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Service.CreateSalonServiceRequest;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;
import com.salon.backend.DTOs.Salon.Service.UpdateSalonServiceRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalonServiceManagementImplTest {

    @Mock private SalonServiceRepo salonServiceRepo;
    @Mock private SalonRepo salonRepo;
    @InjectMocks private SalonServiceManagementImpl service;

    @Test
    void approvedSalonOwnerCanCreateActiveService() {
        Salon salon = activeSalon(10L, 5L);
        CreateSalonServiceRequest request = new CreateSalonServiceRequest(
                "  Haircut  ",
                "  Classic haircut  ",
                new BigDecimal("7.50"),
                30
        );
        when(salonRepo.findByOwnerId(5L)).thenReturn(salon);

        ApiResponse<SalonServiceResponse> response = service.createService(request, 5L);

        assertTrue(response.isSuccess());
        assertEquals("Haircut", response.getData().name());
        assertEquals("Classic haircut", response.getData().description());
        assertTrue(response.getData().isActive());
        assertNotNull(response.getData().createdAt());
        verify(salonServiceRepo).save(any(SalonService.class));
    }

    @Test
    void unapprovedSalonCannotCreateServices() {
        Salon salon = activeSalon(10L, 5L);
        salon.setStatus(SalonStatus.Requested);
        when(salonRepo.findByOwnerId(5L)).thenReturn(salon);

        ApiResponse<SalonServiceResponse> response = service.createService(
                new CreateSalonServiceRequest(
                        "Haircut",
                        null,
                        new BigDecimal("7.50"),
                        30
                ),
                5L
        );

        assertFalse(response.isSuccess());
        verifyNoInteractions(salonServiceRepo);
    }

    @Test
    void publicSalonServicesContainOnlyActiveServices() {
        Salon salon = activeSalon(10L, 5L);
        SalonService salonService = salonService(20L, salon, true);
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(salonServiceRepo.findBySalonIdAndActiveTrueOrderByNameAsc(10L))
                .thenReturn(List.of(salonService));

        ApiResponse<List<SalonServiceResponse>> response =
                service.getActiveSalonServices(10L);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertTrue(response.getData().get(0).isActive());
        verify(salonServiceRepo).findBySalonIdAndActiveTrueOrderByNameAsc(10L);
    }

    @Test
    void blockedSalonIsNotExposedToPublicServiceListing() {
        Salon salon = activeSalon(10L, 5L);
        salon.setBlockedByAdmin(true);
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));

        ApiResponse<List<SalonServiceResponse>> response =
                service.getActiveSalonServices(10L);

        assertFalse(response.isSuccess());
        verifyNoInteractions(salonServiceRepo);
    }

    @Test
    void anotherSalonOwnerCannotUpdateService() {
        when(salonServiceRepo.findByIdAndSalonOwnerId(20L, 6L))
                .thenReturn(Optional.empty());

        ApiResponse<SalonServiceResponse> response = service.updateService(
                20L,
                new UpdateSalonServiceRequest(
                        "Haircut",
                        null,
                        new BigDecimal("8.00"),
                        30,
                        true
                ),
                6L
        );

        assertFalse(response.isSuccess());
        verify(salonServiceRepo, never()).save(any());
    }

    @Test
    void deletingServiceDisablesItWithoutRemovingHistory() {
        Salon salon = activeSalon(10L, 5L);
        SalonService salonService = salonService(20L, salon, true);
        when(salonServiceRepo.findByIdAndSalonOwnerId(20L, 5L))
                .thenReturn(Optional.of(salonService));

        ApiResponse<SalonServiceResponse> response = service.disableService(20L, 5L);

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isActive());
        verify(salonServiceRepo).save(salonService);
        verify(salonServiceRepo, never()).delete(any());
    }

    private Salon activeSalon(Long salonId, long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        Salon salon = new Salon();
        salon.setId(salonId);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        return salon;
    }

    private SalonService salonService(Long serviceId, Salon salon, boolean active) {
        LocalDateTime now = LocalDateTime.now();
        return new SalonService(
                serviceId,
                salon,
                "Haircut",
                "Classic haircut",
                new BigDecimal("7.50"),
                30,
                active,
                now,
                now
        );
    }
}
