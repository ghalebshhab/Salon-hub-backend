package com.salon.backend.DTOs.User.Profile;

import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Entities.salons.SalonStatus;

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
        Long ownedSalonId,
        String ownedSalonName,
        SalonStatus ownedSalonStatus,
        boolean careerProfileEnabled,
        LocalDate dateOfBirth,
        Integer yearsOfExperience,
        List<String> skills,
        List<String> preferredWorkLocations,
        String resumeUrl,
        boolean resumeUploaded,
        String portfolioUrl,
        String professionalBio,
        Integer careerProfileCompletionPercentage,
        String profilePictureUrl,
        /** True when this employee can use the salon manager role now. */
        boolean salonManager
) {
}
