package com.salon.backend.Services.Assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantKnowledgeTest {

    @Test
    void picksTheTopicWithTheMostSharedWords() {
        assertEquals("/my-bookings",
                AssistantKnowledge.bestTopic("how can I cancel my booking").orElseThrow().link().to());
        assertEquals("/account/security",
                AssistantKnowledge.bestTopic("change my password").orElseThrow().link().to());
        assertEquals("/community",
                AssistantKnowledge.bestTopic("how do I follow people in the community").orElseThrow().link().to());
    }

    @Test
    void returnsNothingForUnrelatedText() {
        assertTrue(AssistantKnowledge.bestTopic("weather tomorrow").isEmpty());
    }

    @Test
    void everyTopicLinksToAnAppPage() {
        AssistantKnowledge.TOPICS.forEach(topic -> assertTrue(topic.link().to().startsWith("/")));
    }
}
