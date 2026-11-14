package com.salon.backend.Services.Assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantIntentDetectorTest {

    private static AssistantIntent detect(String message) {
        return AssistantIntentDetector.detect(message);
    }

    @Test
    void rankingQuestionsAskForTopSalons() {
        assertEquals(AssistantIntent.TOP_SALONS, detect("Top rated salons"));
        assertEquals(AssistantIntent.TOP_SALONS, detect("best salon in Irbid"));
        assertEquals(AssistantIntent.TOP_SALONS, detect("أفضل صالون في عمان"));
    }

    @Test
    void placesAndSalonWordsFindSalons() {
        assertEquals(AssistantIntent.FIND_SALONS, detect("salons in Zarqa"));
        assertEquals(AssistantIntent.FIND_SALONS, detect("Aqaba"));
    }

    @Test
    void workQuestionsFindJobs() {
        assertEquals(AssistantIntent.JOBS, detect("open jobs for me"));
        assertEquals(AssistantIntent.JOBS, detect("any vacancy in Amman?"));
        assertEquals(AssistantIntent.JOBS, detect("وظائف في اربد"));
    }

    @Test
    void ownersAskForHiring() {
        assertEquals(AssistantIntent.HIRING, detect("Find employees for my salon"));
        assertEquals(AssistantIntent.HIRING, detect("I need to hire staff"));
    }

    @Test
    void howQuestionsAskForHelp() {
        assertEquals(AssistantIntent.APP_HELP, detect("How do I book?"));
        assertEquals(AssistantIntent.APP_HELP, detect("how does salonhub work"));
        assertEquals(AssistantIntent.APP_HELP, detect("change my password"));
    }

    @Test
    void shortHellosAreGreetings() {
        assertEquals(AssistantIntent.GREETING, detect("hi"));
        assertEquals(AssistantIntent.GREETING, detect("Lamsa"));
        assertEquals(AssistantIntent.GREETING, detect("مرحبا"));
    }

    @Test
    void otherTextIsUnknown() {
        assertEquals(AssistantIntent.UNKNOWN, detect("weather tomorrow"));
        assertEquals(AssistantIntent.UNKNOWN, detect("   "));
    }
}
