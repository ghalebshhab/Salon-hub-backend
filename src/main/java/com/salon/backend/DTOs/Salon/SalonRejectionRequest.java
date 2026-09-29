package com.salon.backend.DTOs.Salon;

public record SalonRejectionRequest(
        Long salonId,
        String rejectionReason
) {}
