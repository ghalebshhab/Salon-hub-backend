package com.salon.backend.Controllers.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Owner.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping
    public ApiResponse<OwnerDashboardResponse> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ownerDashboardService.getDashboard(currentUser.getId());
    }
}
