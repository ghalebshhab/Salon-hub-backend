package com.salon.backend.Services.Salon.Service;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Service.CreateSalonServiceRequest;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;
import com.salon.backend.DTOs.Salon.Service.UpdateSalonServiceRequest;

import java.util.List;

public interface SalonServiceManagement {

    ApiResponse<SalonServiceResponse> createService(
            CreateSalonServiceRequest request,
            Long ownerId
    );

    ApiResponse<List<SalonServiceResponse>> getActiveSalonServices(Long salonId);

    ApiResponse<List<SalonServiceResponse>> getMySalonServices(Long ownerId);

    ApiResponse<SalonServiceResponse> updateService(
            Long serviceId,
            UpdateSalonServiceRequest request,
            Long ownerId
    );

    ApiResponse<SalonServiceResponse> disableService(Long serviceId, Long ownerId);
}
