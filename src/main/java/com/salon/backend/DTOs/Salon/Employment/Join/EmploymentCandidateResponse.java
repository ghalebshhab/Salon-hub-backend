package com.salon.backend.DTOs.Salon.Employment.Join;

import java.util.List;

public record EmploymentCandidateResponse(
        Long userId,
        String fullName,
        String userName,
        Integer yearsOfExperience,
        List<String> skills,
        List<String> preferredWorkLocations,
        String professionalBio,
        String resumeUrl,
        boolean resumeUploaded,
        String portfolioUrl,
        Long currentSalonId,
        String currentSalonName
) {
}
