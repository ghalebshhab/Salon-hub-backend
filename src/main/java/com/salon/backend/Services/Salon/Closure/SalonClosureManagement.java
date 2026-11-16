package com.salon.backend.Services.Salon.Closure;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureRequest;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureResponse;

import java.util.List;

public interface SalonClosureManagement {

    /** Upcoming closures of an active salon, for everyone. */
    ApiResponse<List<SalonClosureResponse>> getUpcomingClosures(Long salonId);

    ApiResponse<SalonClosureResponse> addClosure(Long salonId, SalonClosureRequest request, Long ownerId);

    ApiResponse<Void> removeClosure(Long salonId, Long closureId, Long ownerId);
}
