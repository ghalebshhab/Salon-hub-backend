package com.salon.backend.DTOs.User.Profile;

import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;

import java.time.LocalDate;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String userName,
        String email,
        String phoneNumber,
        UserRole role,
        UserStatus status,
        Long salonId,
        String salonName,
        boolean careerProfileEnabled,
        LocalDate dateOfBirth,
        Integer yearsOfExperience,
        List<String> skills,
        List<String> preferredWorkLocations,
        String resumeUrl,
        boolean resumeUploaded,
        String portfolioUrl,
        String professionalBio,
        Integer careerProfileCompletionPercentage
) {
}
