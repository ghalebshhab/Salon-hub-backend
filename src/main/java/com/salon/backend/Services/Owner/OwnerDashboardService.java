package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;

public interface OwnerDashboardService {

    ApiResponse<OwnerDashboardResponse> getDashboard(Long ownerId);
}
