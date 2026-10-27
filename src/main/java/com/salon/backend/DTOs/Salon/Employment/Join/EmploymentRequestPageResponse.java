package com.salon.backend.DTOs.Salon.Employment.Join;

import java.util.List;

public record EmploymentRequestPageResponse(
        List<EmployeeRequestResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
