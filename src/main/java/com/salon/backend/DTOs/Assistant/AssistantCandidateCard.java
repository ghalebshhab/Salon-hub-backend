package com.salon.backend.DTOs.Assistant;

import java.util.List;

/** A member who turned on a public career profile. */
public record AssistantCandidateCard(
        Long userId,
        String name,
        String userName,
        List<String> skills,
        Integer yearsOfExperience,
        List<String> preferredLocations
) {
}
