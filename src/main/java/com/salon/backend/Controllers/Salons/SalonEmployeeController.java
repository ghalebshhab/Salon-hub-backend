package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Employee.SalonEmployeeDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salon-employees")
@RequiredArgsConstructor
public class SalonEmployeeController {

    private final SalonEmployeeDirectory salonEmployeeDirectory;

    @GetMapping("/salon/{salonId}")
    public ApiResponse<List<SalonEmployeeResponse>> getSalonEmployees(
            @PathVariable Long salonId) {
        return salonEmployeeDirectory.getSalonEmployees(salonId);
    }

    @GetMapping("/my-salon")
    public ApiResponse<List<SalonEmployeeResponse>> getMySalonEmployees(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonEmployeeDirectory.getMySalonEmployees(currentUser.getId());
    }
}
