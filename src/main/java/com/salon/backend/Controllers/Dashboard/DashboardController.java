package com.salon.backend.Controllers.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardSessionResponse;
import com.salon.backend.DTOs.Salon.SalonApprovalRequest;
import com.salon.backend.DTOs.Salon.SalonRejectionRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.Services.Auth.Dashboard.DashboardService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.SalonService;
import com.salon.backend.Services.User.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final SalonService salonService;
    private final UserDirectoryService userService;

    // Auth
    @PostMapping("/login")
    public ApiResponse<DashboardLoginResponse> login(@RequestBody DashboardLoginRequest dashboardLoginRequest) {
        return dashboardService.DashboardLogin(dashboardLoginRequest);
    }

    @GetMapping("/session")
    public ApiResponse<DashboardSessionResponse> session(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "Admin session active",
                new DashboardSessionResponse(currentUser.getId(), currentUser.getEmail(), "ADMIN")
        );
    }

    // Salons
    @GetMapping("/salons/requested")
    public ApiResponse<List<SalonResponse>> getRequestedSalons(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.getRequestedSalons(currentUser.getId());
    }

    @GetMapping("/salons/all")
    public ApiResponse<List<SalonResponse>> getAllSalons(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.getAllSalons(currentUser.getId());
    }

    @GetMapping("/salons/active")
    public ApiResponse<List<SalonResponse>> getActiveSalons() {
        return salonService.getActiveSalons();
    }

    @PostMapping("/salons/requested/accept")
    public ApiResponse<SalonResponse> acceptRequestedSalon(
            @RequestBody SalonApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.acceptSalonRequest(request.salonId(), currentUser.getId());
    }

    @PostMapping("/salons/requested/reject")
    public ApiResponse<SalonResponse> rejectRequestedSalon(
            @RequestBody SalonRejectionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.rejectSalonRequest(
                request.salonId(),
                currentUser.getId(),
                request.rejectionReason()
        );
    }

    @PostMapping("/salons/{salonId}/block")
    public ApiResponse<String> blockSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.blockSalon(salonId, currentUser.getId());
    }

    @PostMapping("/salons/{salonId}/unblock")
    public ApiResponse<String> unblockSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.unblockSalon(salonId, currentUser.getId());
    }

    // Users
    @GetMapping("/users/all")
    public ApiResponse<List<UserSummaryResponse>> getAllUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getUsers(currentUser.getId());
    }

    @GetMapping("/users/active")
    public ApiResponse<List<UserSummaryResponse>> getActiveUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getActiveUsers(currentUser.getId());
    }

    @GetMapping("/users/blocked")
    public ApiResponse<List<UserSummaryResponse>> getBlockedUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getBlockedUsers(currentUser.getId());
    }


    @PostMapping("/users/{userId}/block")
    public ApiResponse<String> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.blockUser(userId, currentUser.getId());
    }

    @PostMapping("/users/{userId}/unblock")
    public ApiResponse<String> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return dashboardService.unblockUser(userId, currentUser.getId());
    }
}
