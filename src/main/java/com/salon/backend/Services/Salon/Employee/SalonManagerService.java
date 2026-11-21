package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.SalonAccess;
import com.salon.backend.Services.Subscription.PlanLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The owner gives or removes the salon manager role. Only the owner can do
 * this, never a manager, and giving the role needs a plan that includes it.
 * Removing it always works, also after a downgrade.
 */
@Service
@RequiredArgsConstructor
public class SalonManagerService {

    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final PlanLimits planLimits;

    @Transactional
    public ApiResponse<Boolean> setManager(Long ownerId, Long employeeId, Boolean manager) {
        if (manager == null) {
            return ApiResponse.error("Say if the employee is a manager");
        }
        Salon salon = ownerId == null ? null : salonRepo.findByOwnerId(ownerId);
        if (!SalonAccess.isActiveSalon(salon)) {
            return ApiResponse.error("Only the owner of an active salon can choose managers");
        }
        if (manager) {
            String planError = planLimits.featureError(salon, PlanFeature.MANAGER_ROLE, "The manager role");
            if (planError != null) {
                return ApiResponse.error(planError);
            }
        }
        User employee = employeeId == null ? null : userRepo.findByIdForUpdate(employeeId).orElse(null);
        if (employee == null
                || employee.getRole() != UserRole.Employee
                || employee.getStatus() != UserStatus.Active
                || employee.getSalon() == null
                || !salon.getId().equals(employee.getSalon().getId())) {
            return ApiResponse.error("Employee of your salon was not found");
        }
        employee.setSalonManager(manager);
        userRepo.save(employee);
        String name = employee.getFirstName() == null ? "The employee" : employee.getFirstName().trim();
        return ApiResponse.success(manager
                ? name + " is now a manager"
                : name + " is no longer a manager", manager);
    }
}
