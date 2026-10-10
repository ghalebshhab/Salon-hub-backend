package com.salon.backend.Services.Salon.Employee;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonEmployeeDirectoryImpl implements SalonEmployeeDirectory {

    private final SalonRepo salonRepo;
    private final UserRepo userRepo;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonEmployeeResponse>> getSalonEmployees(Long salonId) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("Active salon not found");
        }
        return ApiResponse.success("Salon employees fetched successfully", findEmployees(salon));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonEmployeeResponse>> getMySalonEmployees(Long ownerId) {
        Salon salon = ownerId == null ? null : salonRepo.findByOwnerId(ownerId);
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        return ApiResponse.success("Salon employees fetched successfully", findEmployees(salon));
    }

    private List<SalonEmployeeResponse> findEmployees(Salon salon) {
        return userRepo.findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
                        salon.getId(),
                        UserRole.Employee,
                        UserStatus.Active
                )
                .stream()
                .map(employee -> mapEmployee(employee, salon))
                .toList();
    }

    private SalonEmployeeResponse mapEmployee(User employee, Salon salon) {
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) {
            fullName = employee.getUserName();
        }

        String specialty = employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                ? null
                : employee.getCareerSkills().get(0);

        return new SalonEmployeeResponse(
                employee.getId(),
                fullName,
                employee.getEmail(),
                employee.getPhoneNumber(),
                specialty,
                null,
                "ACTIVE",
                salon.getOpeningTime(),
                salon.getClosingTime()
        );
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }
}
