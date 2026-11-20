package com.salon.backend.Controllers.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Subscription.AdminPlanRequestResponse;
import com.salon.backend.DTOs.Subscription.AdminSalonPlanResponse;
import com.salon.backend.DTOs.Subscription.ChangePlanRequest;
import com.salon.backend.DTOs.Subscription.GrantPlanRequest;
import com.salon.backend.DTOs.Subscription.RejectPlanRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Subscription.AdminSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin: salon plan requests and plan changes. ADMIN only (see SecurityConfig). */
@RestController
@RequestMapping("/api/dashboard/subscriptions")
@RequiredArgsConstructor
public class DashboardSubscriptionController {

    private final AdminSubscriptionService subscriptionService;

    /** Requests waiting for review, oldest first. */
    @GetMapping("/requests")
    public ApiResponse<List<AdminPlanRequestResponse>> requests() {
        return subscriptionService.getRequests(false);
    }

    /** The latest 100 requests of every status, newest first. */
    @GetMapping("/requests/history")
    public ApiResponse<List<AdminPlanRequestResponse>> history() {
        return subscriptionService.getRequests(true);
    }

    @GetMapping("/salons")
    public ApiResponse<List<AdminSalonPlanResponse>> salons() {
        return subscriptionService.getSalons();
    }

    @PostMapping("/requests/{requestId}/confirm")
    public ApiResponse<AdminPlanRequestResponse> confirm(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return subscriptionService.confirm(requestId, currentUser.getId());
    }

    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<AdminPlanRequestResponse> reject(
            @PathVariable Long requestId,
            @RequestBody RejectPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return subscriptionService.reject(requestId, request, currentUser.getId());
    }

    @PostMapping("/salons/{salonId}/grant")
    public ApiResponse<AdminSalonPlanResponse> grant(
            @PathVariable Long salonId,
            @RequestBody GrantPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return subscriptionService.grant(salonId, request, currentUser.getId());
    }

    @PostMapping("/salons/{salonId}/plan")
    public ApiResponse<AdminSalonPlanResponse> changePlan(
            @PathVariable Long salonId,
            @RequestBody ChangePlanRequest request) {
        return subscriptionService.changePlan(salonId, request);
    }
}
