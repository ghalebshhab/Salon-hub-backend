package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantCandidateCard;
import com.salon.backend.DTOs.Assistant.AssistantGovernorateRanking;
import com.salon.backend.DTOs.Assistant.AssistantJobCard;
import com.salon.backend.DTOs.Assistant.AssistantLink;
import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import com.salon.backend.DTOs.Assistant.AssistantTurn;

import java.util.List;
import java.util.Optional;

/** Everything a reply writer may use. The records are already loaded. */
public record AssistantReplyContext(
        String message,
        List<AssistantTurn> history,
        AssistantIntent intent,
        Optional<JordanGovernorate> governorate,
        Viewer viewer,
        List<AssistantSalonCard> salons,
        List<AssistantJobCard> jobs,
        List<AssistantCandidateCard> candidates,
        List<AssistantGovernorateRanking> rankings,
        Optional<AssistantKnowledge.Topic> topic,
        List<AssistantLink> links,
        Optional<AssistantSalonCard> ownedSalon
) {

    /** Who is asking. Guests have no id and no name. */
    public record Viewer(Long id, String firstName, String role, boolean careerProfile) {

        public static Viewer guest() {
            return new Viewer(null, null, "GUEST", false);
        }

        public boolean isGuest() {
            return id == null;
        }

        public boolean isOwner() {
            return "OWNER".equals(role);
        }
    }
}
