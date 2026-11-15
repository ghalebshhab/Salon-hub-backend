package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employee.SalonEmployeeResponse;
import com.salon.backend.DTOs.Salon.Employee.PublicSalonEmployeeResponse;
import com.salon.backend.DTOs.Salon.Employee.EmployeeCapabilitiesResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeServicesRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeScheduleResponse;
import com.salon.backend.DTOs.Salon.Employee.UpdateEmployeeScheduleRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffRequest;
import com.salon.backend.DTOs.Salon.Employee.EmployeeTimeOffResponse;
import com.salon.backend.Services.Salon.Employee.EmployeeTimeOffManagement;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Employee.EmployeeCapabilityManagement;
import com.salon.backend.Services.Salon.Employee.SalonEmployeeDirectory;
import com.salon.backend.Services.Salon.Employee.EmployeeScheduleManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/salon-employees")
@RequiredArgsConstructor
public class SalonEmployeeController {

    private final SalonEmployeeDirectory salonEmployeeDirectory;
    private final EmployeeCapabilityManagement employeeCapabilityManagement;
    private final EmployeeScheduleManagement employeeScheduleManagement;
    private final EmployeeTimeOffManagement employeeTimeOffManagement;

    @GetMapping("/salon/{salonId}")
    public ApiResponse<List<PublicSalonEmployeeResponse>> getSalonEmployees(
            @PathVariable Long salonId) {
        return salonEmployeeDirectory.getSalonEmployees(salonId);
    }

    @GetMapping("/my-salon")
    public ApiResponse<List<SalonEmployeeResponse>> getMySalonEmployees(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonEmployeeDirectory.getMySalonEmployees(currentUser.getId());
    }

    @GetMapping("/{employeeId}/services")
    public ApiResponse<EmployeeCapabilitiesResponse> getEmployeeServices(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeCapabilityManagement.getEmployeeServices(employeeId, currentUser.getId());
    }

    @PutMapping("/{employeeId}/services")
    public ApiResponse<EmployeeCapabilitiesResponse> updateEmployeeServices(
            @PathVariable Long employeeId,
            @RequestBody UpdateEmployeeServicesRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeCapabilityManagement.updateEmployeeServices(
                employeeId,
                request,
                currentUser.getId()
        );
    }

    @GetMapping("/{employeeId}/schedule")
    public ApiResponse<EmployeeScheduleResponse> getEmployeeSchedule(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeScheduleManagement.getEmployeeSchedule(employeeId, currentUser.getId());
    }

    @PutMapping("/{employeeId}/schedule")
    public ApiResponse<EmployeeScheduleResponse> updateEmployeeSchedule(
            @PathVariable Long employeeId,
            @RequestBody UpdateEmployeeScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeScheduleManagement.updateEmployeeSchedule(
                employeeId,
                request,
                currentUser.getId()
        );
    }

    @GetMapping("/{employeeId}/time-off")
    public ApiResponse<List<EmployeeTimeOffResponse>> getEmployeeTimeOff(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeTimeOffManagement.getUpcomingTimeOff(employeeId, currentUser.getId());
    }

    @PostMapping("/{employeeId}/time-off")
    public ApiResponse<EmployeeTimeOffResponse> addEmployeeTimeOff(
            @PathVariable Long employeeId,
            @RequestBody EmployeeTimeOffRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeTimeOffManagement.addTimeOff(employeeId, request, currentUser.getId());
    }

    @DeleteMapping("/time-off/{timeOffId}")
    public ApiResponse<Void> removeEmployeeTimeOff(
            @PathVariable Long timeOffId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employeeTimeOffManagement.removeTimeOff(timeOffId, currentUser.getId());
    }
}
