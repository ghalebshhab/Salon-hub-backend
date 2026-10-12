package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeCapabilitiesResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeServicesRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.services.EmployeeServiceCapability;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import com.salon.backend.Repositories.User.UserRepo;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeCapabilityManagementImplTest {

    @Mock private EmployeeServiceCapabilityRepo capabilityRepo;
    @Mock private SalonServiceRepo salonServiceRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @InjectMocks private EmployeeCapabilityManagementImpl capabilityManagement;

    @Test
    void ownerCanReplaceEmployeeCapabilitiesWithActiveOwnedServices() {
        Salon salon = activeSalon(10L, 2L);
        User employee = employee(4L, salon);
        SalonService haircut = service(20L, salon, "Haircut");
        SalonService color = service(21L, salon, "Color");
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L, 21L), 10L))
                .thenReturn(List.of(haircut, color));

        ApiResponse<EmployeeCapabilitiesResponse> response = capabilityManagement.updateEmployeeServices(
                4L,
                new UpdateEmployeeServicesRequest(List.of(20L, 21L)),
                2L
        );

        assertTrue(response.isSuccess());
        assertEquals(List.of("Color", "Haircut"), response.getData().services().stream()
                .map(item -> item.name()).toList());
        verify(capabilityRepo).deleteByEmployeeId(4L);
        verify(capabilityRepo).flush();
        verify(capabilityRepo).saveAll(any());
    }

    @Test
    void emptyListClearsCapabilitiesWithoutCreatingRows() {
        Salon salon = activeSalon(10L, 2L);
        User employee = employee(4L, salon);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee));

        ApiResponse<EmployeeCapabilitiesResponse> response = capabilityManagement.updateEmployeeServices(
                4L,
                new UpdateEmployeeServicesRequest(List.of()),
                2L
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getData().services().isEmpty());
        verify(capabilityRepo).deleteByEmployeeId(4L);
        verify(capabilityRepo, never()).saveAll(any());
    }

    @Test
    void ownerCannotManageEmployeeFromAnotherSalon() {
        Salon ownedSalon = activeSalon(10L, 2L);
        Salon otherSalon = activeSalon(11L, 3L);
        when(salonRepo.findByOwnerId(2L)).thenReturn(ownedSalon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(4L, otherSalon)));

        ApiResponse<EmployeeCapabilitiesResponse> response = capabilityManagement.updateEmployeeServices(
                4L,
                new UpdateEmployeeServicesRequest(List.of()),
                2L
        );

        assertFalse(response.isSuccess());
        verify(capabilityRepo, never()).deleteByEmployeeId(any());
    }

    @Test
    void duplicateServiceIdsAreRejectedBeforeExistingCapabilitiesChange() {
        Salon salon = activeSalon(10L, 2L);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(4L, salon)));

        ApiResponse<EmployeeCapabilitiesResponse> response = capabilityManagement.updateEmployeeServices(
                4L,
                new UpdateEmployeeServicesRequest(List.of(20L, 20L)),
                2L
        );

        assertFalse(response.isSuccess());
        verify(capabilityRepo, never()).deleteByEmployeeId(any());
    }

    @Test
    void inactiveOrForeignServiceIsRejectedBeforeReplacement() {
        Salon salon = activeSalon(10L, 2L);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(userRepo.findByIdForUpdate(4L)).thenReturn(Optional.of(employee(4L, salon)));
        when(salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(List.of(20L, 21L), 10L))
                .thenReturn(List.of(service(20L, salon, "Haircut")));

        ApiResponse<EmployeeCapabilitiesResponse> response = capabilityManagement.updateEmployeeServices(
                4L,
                new UpdateEmployeeServicesRequest(List.of(20L, 21L)),
                2L
        );

        assertFalse(response.isSuccess());
        verify(capabilityRepo, never()).deleteByEmployeeId(any());
    }

    private Salon activeSalon(Long id, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        owner.setRole(UserRole.OWNER);
        owner.setStatus(UserStatus.Active);

        Salon salon = new Salon();
        salon.setId(id);
        salon.setName("Salon " + id);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        owner.setSalon(salon);
        return salon;
    }

    private User employee(Long id, Salon salon) {
        User employee = new User();
        employee.setId(id);
        employee.setFirstName("Lina");
        employee.setLastName("Stylist");
        employee.setRole(UserRole.Employee);
        employee.setStatus(UserStatus.Active);
        employee.setSalon(salon);
        return employee;
    }

    private SalonService service(Long id, Salon salon, String name) {
        LocalDateTime now = LocalDateTime.now();
        return new SalonService(
                id,
                salon,
                name,
                null,
                new BigDecimal("10.00"),
                30,
                true,
                now,
                now
        );
    }
}
