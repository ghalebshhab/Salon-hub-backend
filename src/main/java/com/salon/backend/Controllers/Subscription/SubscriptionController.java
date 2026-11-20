package com.salon.backend.Controllers.Subscription;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Subscription.CreateSubscriptionRequest;
import com.salon.backend.DTOs.Subscription.PlanResponse;
import com.salon.backend.DTOs.Subscription.SubscriptionResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Subscription.OwnerPlanService;
import com.salon.backend.Services.Subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The public price list and the owner's own plan. */
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final OwnerPlanService ownerPlanService;

    /** Public: prices, limits and features of every plan. */
    @GetMapping("/api/subscription-plans")
    public ApiResponse<List<PlanResponse>> getPlans() {
        return ApiResponse.success("Plans fetched successfully", subscriptionService.catalog());
    }

    @GetMapping("/api/owner/subscription")
    public ApiResponse<SubscriptionResponse> getMine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ownerPlanService.getMine(currentUser.getId());
    }

    /** The owner asks for a paid plan; an admin turns it on after the payment. */
    @PostMapping("/api/owner/subscription/requests")
    public ApiResponse<SubscriptionResponse> requestPlan(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody CreateSubscriptionRequest request) {
        return ownerPlanService.requestPlan(currentUser.getId(), request);
    }

    @DeleteMapping("/api/owner/subscription/requests/{requestId}")
    public ApiResponse<SubscriptionResponse> cancelRequest(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long requestId) {
        return ownerPlanService.cancelRequest(currentUser.getId(), requestId);
    }
}
