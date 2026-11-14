package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import com.salon.backend.DTOs.Assistant.AssistantTurn;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeReplyWriterTest {

    private static AssistantReplyContext greeting() {
        return GuideReplyWriterTest.context(AssistantIntent.GREETING, Optional.empty(),
                AssistantReplyContext.Viewer.guest(), List.of(), List.of(), Optional.empty());
    }

    @Test
    void isOffWithoutAnApiKey() {
        ClaudeReplyWriter writer = new ClaudeReplyWriter("", "claude-opus-5-5");
        assertFalse(writer.isEnabled());
        assertTrue(writer.write(greeting()).isEmpty());
    }

    @Test
    void historyStartsWithAUserTurnAndKeepsTheLatestTurns() {
        List<AssistantTurn> history = new ArrayList<>();
        history.add(new AssistantTurn("assistant", "Welcome!"));
        for (int i = 0; i < 10; i++) {
            history.add(new AssistantTurn(i % 2 == 0 ? "user" : "assistant", "turn " + i));
        }
        List<AssistantTurn> usable = ClaudeReplyWriter.usableHistory(history);
        assertEquals("user", usable.get(0).role());
        assertTrue(usable.size() <= ClaudeReplyWriter.HISTORY_TURNS);
        assertEquals("turn 9", usable.get(usable.size() - 1).text());
    }

    @Test
    void recordsCarryOnlyTheLoadedData() {
        var salon = new AssistantSalonCard(1L, "Noor Studio", "Irbid", "Irbid", null, 4.6, 9, true, 1L);
        String records = ClaudeReplyWriter.records(GuideReplyWriterTest.context(AssistantIntent.TOP_SALONS,
                Optional.of(JordanGovernorate.IRBID), AssistantReplyContext.Viewer.guest(),
                List.of(salon), List.of(), Optional.empty()));
        assertTrue(records.startsWith("<records>"));
        assertTrue(records.endsWith("</records>"));
        assertTrue(records.contains("viewer: guest"));
        assertTrue(records.contains("governorate: Irbid"));
        assertTrue(records.contains("Noor Studio (Irbid, 4.6 stars, 9 reviews, free team seats 1)"), records);
    }

    @Test
    void requestUsesTheConfiguredModelAndRefusalFallback() {
        ClaudeReplyWriter writer = new ClaudeReplyWriter("test-key", "claude-opus-5-5");
        var params = writer.buildParams(greeting());
        assertEquals("claude-opus-5-5", params.model().asString());
        assertEquals(1, params.messages().size());
        assertTrue(params.betas().orElseThrow().stream()
                .anyMatch(beta -> beta.toString().contains("server-side-fallback")));
    }
}
