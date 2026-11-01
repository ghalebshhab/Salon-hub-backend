package com.salon.backend.DTOs.Salon.Employee;

import java.util.List;

public record PublicSalonEmployeeResponse(
        Long id,
        String fullName,
        String specialty,
        String imageUrl,
        Integer yearsOfExperience,
        List<String> skills,
        String professionalBio,
        List<Long> serviceIds,
        List<String> serviceNames
) {}
