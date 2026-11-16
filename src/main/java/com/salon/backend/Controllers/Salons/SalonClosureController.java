package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureRequest;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Closure.SalonClosureManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Salon holidays and other whole-day closures. Reading is public; changes are owner only. */
@RestController
@RequestMapping("/api/salons/{salonId}/closures")
@RequiredArgsConstructor
public class SalonClosureController {

    private final SalonClosureManagement closureManagement;

    @GetMapping
    public ApiResponse<List<SalonClosureResponse>> getUpcomingClosures(@PathVariable Long salonId) {
        return closureManagement.getUpcomingClosures(salonId);
    }

    @PostMapping
    public ApiResponse<SalonClosureResponse> addClosure(
            @PathVariable Long salonId,
            @RequestBody SalonClosureRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return closureManagement.addClosure(salonId, request, currentUser.getId());
    }

    @DeleteMapping("/{closureId}")
    public ApiResponse<Void> removeClosure(
            @PathVariable Long salonId,
            @PathVariable Long closureId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return closureManagement.removeClosure(salonId, closureId, currentUser.getId());
    }
}
