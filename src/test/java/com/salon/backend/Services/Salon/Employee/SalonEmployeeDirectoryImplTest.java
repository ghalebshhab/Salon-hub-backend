package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.services.EmployeeServiceCapability;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalonEmployeeDirectoryImplTest {

    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @Mock private EmployeeServiceCapabilityRepo capabilityRepo;
    @InjectMocks private SalonEmployeeDirectoryImpl directory;

    @Test
    void employeeDirectoryIncludesOnlyActiveServiceCapabilities() {
        Salon salon = activeSalon(10L);
        User employee = employee(4L, salon);
        SalonService haircut = service(20L, salon, "Haircut", true);
        SalonService retired = service(21L, salon, "Retired service", false);
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(userRepo.findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
                10L,
                UserRole.Employee,
                UserStatus.Active
        )).thenReturn(List.of(employee));
        when(capabilityRepo.findByEmployeeIdIn(List.of(4L))).thenReturn(List.of(
                new EmployeeServiceCapability(1L, employee, haircut, LocalDateTime.now()),
                new EmployeeServiceCapability(2L, employee, retired, LocalDateTime.now())
        ));

        ApiResponse<List<SalonEmployeeResponse>> response = directory.getSalonEmployees(10L);

        assertTrue(response.isSuccess());
        assertEquals(List.of(20L), response.getData().get(0).serviceIds());
        assertEquals(List.of("Haircut"), response.getData().get(0).serviceNames());
    }

    @Test
    void blockedSalonDoesNotExposeItsEmployeeDirectory() {
        Salon salon = activeSalon(10L);
        salon.setBlockedByAdmin(true);
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));

        ApiResponse<List<SalonEmployeeResponse>> response = directory.getSalonEmployees(10L);

        assertFalse(response.isSuccess());
        verifyNoInteractions(userRepo, capabilityRepo);
    }

    private Salon activeSalon(Long id) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
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

    private SalonService service(Long id, Salon salon, String name, boolean active) {
        LocalDateTime now = LocalDateTime.now();
        return new SalonService(
                id,
                salon,
                name,
                null,
                new BigDecimal("10.00"),
                30,
                active,
                now,
                now
        );
    }
}
