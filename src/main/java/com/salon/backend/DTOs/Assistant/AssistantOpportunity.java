package com.salon.backend.DTOs.Assistant;

import java.util.List;

/**
 * A suggestion that fits the viewer's role: jobs for job seekers, candidates
 * and free seats for owners, salons that hire for everyone else.
 */
public record AssistantOpportunity(
        String audience,
        String title,
        String summary,
        AssistantLink action,
        List<AssistantJobCard> jobs,
        List<AssistantSalonCard> salons,
        List<AssistantCandidateCard> candidates
) {
}
