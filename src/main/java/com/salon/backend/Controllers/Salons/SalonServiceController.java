package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Service.CreateSalonServiceRequest;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;
import com.salon.backend.DTOs.Salon.Service.UpdateSalonServiceRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Service.SalonServiceManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salon-services")
@RequiredArgsConstructor
public class SalonServiceController {

    private final SalonServiceManagement salonServiceManagement;

    @PostMapping
    public ApiResponse<SalonServiceResponse> createService(
            @RequestBody CreateSalonServiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonServiceManagement.createService(request, currentUser.getId());
    }

    @GetMapping("/salon/{salonId}")
    public ApiResponse<List<SalonServiceResponse>> getActiveSalonServices(
            @PathVariable Long salonId) {
        return salonServiceManagement.getActiveSalonServices(salonId);
    }

    @GetMapping("/my-salon")
    public ApiResponse<List<SalonServiceResponse>> getMySalonServices(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonServiceManagement.getMySalonServices(currentUser.getId());
    }

    @PutMapping("/{serviceId}")
    public ApiResponse<SalonServiceResponse> updateService(
            @PathVariable Long serviceId,
            @RequestBody UpdateSalonServiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonServiceManagement.updateService(serviceId, request, currentUser.getId());
    }

    @DeleteMapping("/{serviceId}")
    public ApiResponse<SalonServiceResponse> disableService(
            @PathVariable Long serviceId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonServiceManagement.disableService(serviceId, currentUser.getId());
    }
}
