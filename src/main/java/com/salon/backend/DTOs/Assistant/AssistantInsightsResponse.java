package com.salon.backend.DTOs.Assistant;

import java.util.List;

public record AssistantInsightsResponse(
        List<AssistantGovernorateRanking> rankings,
        AssistantOpportunity opportunity
) {
}
