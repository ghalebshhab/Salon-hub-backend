package com.salon.backend.DTOs.Assistant;

import java.util.List;

/**
 * The assistant's answer. The reply text explains; the cards hold the real
 * records it is based on. Mode is "ai" when a language model wrote the reply
 * and "guide" when the built-in guide did.
 */
public record AssistantChatResponse(
        String reply,
        String intent,
        String mode,
        List<AssistantSalonCard> salons,
        List<AssistantJobCard> jobs,
        List<AssistantCandidateCard> candidates,
        List<AssistantGovernorateRanking> rankings,
        List<AssistantLink> links,
        List<String> suggestions
) {
}
