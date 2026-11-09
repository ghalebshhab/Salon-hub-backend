package com.salon.backend.DTOs.Community;

import com.salon.backend.Entities.users.UserRole;

import java.util.List;

public record CommunityProfileResponse(
        CommunityMemberResponse member,
        UserRole role,
        boolean careerProfileEnabled,
        Integer yearsOfExperience,
        List<String> skills,
        List<String> preferredWorkLocations,
        String portfolioUrl,
        String professionalBio,
        Long salonId,
        String salonName
) {}
