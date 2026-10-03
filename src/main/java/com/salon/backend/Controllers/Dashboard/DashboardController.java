package com.salon.backend.Controllers.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginResponse;
import com.salon.backend.DTOs.Salon.SalonApprovalRequest;
import com.salon.backend.DTOs.Salon.SalonRejectionRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Services.Auth.Dashboard.DashboardService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.SalonService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
    @PostMapping("/login")
    public ApiResponse<DashboardLoginResponse> login(@RequestBody DashboardLoginRequest dashboardLoginRequest){
      return  dashboardService.DashboardLogin(dashboardLoginRequest);
    }

    @GetMapping("/salons/requested")
    public ApiResponse<List<SalonResponse>> requestedSalons(
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return salonService.getRequestedSalons(currentUser.getId());
    }
    @PostMapping("/salons/requested/accept")
    public ApiResponse<SalonResponse> acceptRequestedSalons(
            @RequestBody SalonApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.acceptSalonRequest(request.salonId(), currentUser.getId());
    }
    @PostMapping("/salons/requested/reject")
    public ApiResponse<SalonResponse> rejectRequestedSalons(
            @RequestBody SalonRejectionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.rejectSalonRequest(
                request.salonId(),
                currentUser.getId(),
                request.rejectionReason()
        );
    }
    @PostMapping("/users/{userId}/block")
    public ApiResponse<String> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.blockUser(userId, currentUser.getId());
    }

    @PostMapping("/users/{userId}/unblock")
    public ApiResponse<String> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.unblockUser(userId, currentUser.getId());
    }

    @PostMapping("/salons/{salonId}/block")
    public ApiResponse<String> blockSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.blockSalon(salonId, currentUser.getId());
    }

    @PostMapping("/salons/{salonId}/unblock")
    public ApiResponse<String> unblockSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser){
        return dashboardService.unblockSalon(salonId, currentUser.getId());
    }

}
