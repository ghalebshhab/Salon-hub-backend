package com.salon.backend.DTOs.Media;

import java.util.List;

public record EmployeeWorkMediaPageResponse(
        List<EmployeeWorkMediaResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}
