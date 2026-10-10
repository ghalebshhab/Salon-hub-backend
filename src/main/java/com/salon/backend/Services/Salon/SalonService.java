package com.salon.backend.Services.Salon;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.CreateSalonRequest;
import com.salon.backend.DTOs.Salon.CreateSalonResponse;
import com.salon.backend.DTOs.Salon.UpdateSalonRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;

import java.util.List;

public interface SalonService {
    ApiResponse<CreateSalonResponse> createSalon(CreateSalonRequest createSalonRequest,Long id);
    ApiResponse<CreateSalonResponse> updateSalon(UpdateSalonRequest updateSalonRequest,Long id,Long ownerId);
    ApiResponse<Void> deleteSalon(long salonId, Long ownerId);
    ApiResponse<SalonResponse> getSalonById(long id);
    ApiResponse<SalonResponse> getSalonByOwnerId(Long ownerId);
    ApiResponse<List<SalonResponse>> getAllSalons(Long adminId);
    ApiResponse<List<SalonResponse>> getActiveSalons();
    ApiResponse<List<SalonResponse>> getRequestedSalons(Long adminId);
    ApiResponse<SalonResponse> resubmitRejectedSalon(Long salonId, Long ownerId);

}
