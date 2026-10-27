package com.salon.backend.DTOs.Salon.Employment.Leave;

import java.util.List;

public record LeaveRequestPageResponse(
        List<LeaveRequestResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
