package com.salon.backend.DTOs.Salon.HiringPost;

import com.salon.backend.Entities.salons.hiringposts.ApplicationMatchLevel;

import java.util.List;

public record HiringPostDiscoveryResponse(
        HiringPostResponse hiringPost,
        ApplicationMatchLevel matchLevel,
        Integer matchScore,
        List<String> matchedSkills,
        List<String> additionalSkills,
        List<String> missingSkills,
        List<String> missingRequirements
) {
}
