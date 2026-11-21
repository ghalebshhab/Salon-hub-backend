package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.Services.Salon.SalonAccess;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeCapabilitiesResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeCapabilityServiceResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeCapabilityManagementImpl implements EmployeeCapabilityManagement {

    private final EmployeeServiceCapabilityRepo capabilityRepo;
    private final SalonServiceRepo salonServiceRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final SalonAccess salonAccess;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmployeeCapabilitiesResponse> getEmployeeServices(
            Long employeeId,
            Long ownerId) {

        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }

        User employee = findSalonEmployee(employeeId, salon.getId(), false);
        if (employee == null) {
            return ApiResponse.error("Employee in your salon was not found");
        }

        return ApiResponse.success(
                "Employee service capabilities fetched successfully",
                mapResponse(
                        employee,
                        capabilityRepo.findByEmployeeIdAndServiceSalonIdOrderByServiceNameAsc(
                                employeeId,
                                salon.getId()
                        )
                )
        );
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeCapabilitiesResponse> updateEmployeeServices(
            Long employeeId,
            UpdateEmployeeServicesRequest request,
            Long ownerId) {

        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }

        User employee = findSalonEmployee(employeeId, salon.getId(), true);
        if (employee == null) {
            return ApiResponse.error("Active employee in your salon was not found");
        }
        if (request == null || request.serviceIds() == null) {
            return ApiResponse.error("Service ids are required; use an empty list to clear capabilities");
        }
        if (request.serviceIds().stream().anyMatch(id -> id == null)) {
            return ApiResponse.error("Every service id is required");
        }

        Set<Long> uniqueServiceIds = new LinkedHashSet<>(request.serviceIds());
        if (uniqueServiceIds.size() != request.serviceIds().size()) {
            return ApiResponse.error("A service can be assigned only once to an employee");
        }

        List<SalonService> services = uniqueServiceIds.isEmpty()
                ? List.of()
                : salonServiceRepo.findByIdInAndSalonIdAndActiveTrue(
                        List.copyOf(uniqueServiceIds),
                        salon.getId()
                );
        if (services.size() != uniqueServiceIds.size()) {
            return ApiResponse.error("Every assigned service must be active and belong to your salon");
        }

        capabilityRepo.deleteByEmployeeId(employeeId);
        capabilityRepo.flush();

        LocalDateTime now = LocalDateTime.now();
        List<EmployeeServiceCapability> capabilities = services.stream()
                .map(service -> new EmployeeServiceCapability(null, employee, service, now))
                .toList();
        if (!capabilities.isEmpty()) {
            capabilityRepo.saveAll(capabilities);
        }

        return ApiResponse.success(
                "Employee service capabilities updated successfully",
                mapResponse(employee, capabilities)
        );
    }

    /** The owner's salon, or the salon of a manager (Business plan). */
    private Salon findManageableSalon(Long ownerId) {
        Salon salon = ownerId == null ? null : salonRepo.findByOwnerId(ownerId);
        if (salon != null) {
            return isActiveSalon(salon) ? salon : null;
        }
        return salonAccess.managerSalon(ownerId);
    }

    private User findSalonEmployee(Long employeeId, Long salonId, boolean activeRequired) {
        if (employeeId == null) {
            return null;
        }
        User employee = activeRequired
                ? userRepo.findByIdForUpdate(employeeId).orElse(null)
                : userRepo.findById(employeeId).orElse(null);
        if (employee == null
                || employee.getRole() != UserRole.Employee
                || employee.getSalon() == null
                || !employee.getSalon().getId().equals(salonId)
                || (activeRequired && employee.getStatus() != UserStatus.Active)) {
            return null;
        }
        return employee;
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private EmployeeCapabilitiesResponse mapResponse(
            User employee,
            List<EmployeeServiceCapability> capabilities) {

        List<EmployeeCapabilityServiceResponse> services = capabilities.stream()
                .map(EmployeeServiceCapability::getService)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(service -> new EmployeeCapabilityServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getPrice(),
                        service.getDurationMinutes(),
                        service.isActive()
                ))
                .toList();

        return new EmployeeCapabilitiesResponse(
                employee.getId(),
                fullName(employee),
                employee.getSalon().getId(),
                services
        );
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserName() : fullName;
    }
}
