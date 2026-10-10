package com.salon.backend.Controllers.Salons;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.CreateSalonRequest;
import com.salon.backend.DTOs.Salon.CreateSalonResponse;
import com.salon.backend.DTOs.Salon.UpdateSalonRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.SalonService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons")
@RequiredArgsConstructor
public class SalonController {
    private final SalonService salonService;
    @GetMapping
    public ApiResponse<List<SalonResponse>> getAllSalons(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.getAllSalons(currentUser.getId());
    }
    @GetMapping("/active")
    public ApiResponse<List<SalonResponse>> getActiveSalons() {
        return salonService.getActiveSalons();
    }
    @GetMapping("/{salonId}")
    public ApiResponse<SalonResponse> getSalon(@PathVariable Long salonId) {
        return salonService.getSalonById(salonId);
    }
    @GetMapping("/mine")
    public ApiResponse<SalonResponse> getMySalon(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.getSalonByOwnerId(currentUser.getId());
    }
    @PostMapping("/create")
    public ApiResponse<CreateSalonResponse> createSalon(
            @RequestBody CreateSalonRequest createSalonRequest,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.createSalon(createSalonRequest, currentUser.getId());
    }
    @PutMapping("/{salonId}/edit")
    public ApiResponse<CreateSalonResponse> updateSalon(
            @RequestBody UpdateSalonRequest updateSalonRequest,
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.updateSalon(updateSalonRequest, salonId, currentUser.getId());
    }

    @PostMapping("/{salonId}/resubmit")
    public ApiResponse<SalonResponse> resubmitSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.resubmitRejectedSalon(salonId, currentUser.getId());
    }

    @DeleteMapping("/{salonId}")
    public ApiResponse<Void> deleteSalon(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonService.deleteSalon(salonId, currentUser.getId());
    }




}
