package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantCandidateCard;
import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideReplyWriterTest {

    static AssistantReplyContext context(AssistantIntent intent, Optional<JordanGovernorate> place,
                                         AssistantReplyContext.Viewer viewer,
                                         List<AssistantSalonCard> salons,
                                         List<AssistantCandidateCard> candidates,
                                         Optional<AssistantSalonCard> owned) {
        return new AssistantReplyContext("question", List.of(), intent, place, viewer, salons, List.of(),
                candidates, List.of(), Optional.empty(), List.of(), owned);
    }

    private static AssistantSalonCard salon(String name, Double rating, long reviews, Long seats) {
        return new AssistantSalonCard(1L, name, "Irbid", "Irbid", null, rating, reviews, true, seats);
    }

    @Test
    void namesTheBestRatedSalonOfTheGovernorate() {
        String reply = GuideReplyWriter.write(context(AssistantIntent.TOP_SALONS,
                Optional.of(JordanGovernorate.IRBID), AssistantReplyContext.Viewer.guest(),
                List.of(salon("Noor Studio", 4.8, 12, null)), List.of(), Optional.empty()));
        assertTrue(reply.contains("Noor Studio"), reply);
        assertTrue(reply.contains("4.8 stars from 12 reviews"), reply);
    }

    @Test
    void saysSoWhenThereAreNoSalons() {
        String reply = GuideReplyWriter.write(context(AssistantIntent.FIND_SALONS,
                Optional.of(JordanGovernorate.TAFILAH), AssistantReplyContext.Viewer.guest(),
                List.of(), List.of(), Optional.empty()));
        assertTrue(reply.contains("no approved salons in Tafilah"), reply);
    }

    @Test
    void tellsOwnersAboutFreeSeats() {
        var owner = new AssistantReplyContext.Viewer(3L, "Rana", "OWNER", false);
        String reply = GuideReplyWriter.write(context(AssistantIntent.HIRING, Optional.empty(), owner,
                List.of(), List.of(), Optional.of(salon("Rana Salon", null, 0, 2L))));
        assertTrue(reply.contains("Rana Salon has 2 free seats"), reply);
    }

    @Test
    void sendsNonOwnersToJobs() {
        String reply = GuideReplyWriter.write(context(AssistantIntent.HIRING, Optional.empty(),
                AssistantReplyContext.Viewer.guest(), List.of(), List.of(), Optional.empty()));
        assertTrue(reply.contains("Hiring is for salon owners"), reply);
    }

    @Test
    void greetsByFirstName() {
        var member = new AssistantReplyContext.Viewer(4L, "Omar", "USER", false);
        String reply = GuideReplyWriter.write(context(AssistantIntent.GREETING, Optional.empty(), member,
                List.of(), List.of(), Optional.empty()));
        assertTrue(reply.startsWith("Welcome, Omar! I am " + AssistantIdentity.NAME), reply);
    }
}
