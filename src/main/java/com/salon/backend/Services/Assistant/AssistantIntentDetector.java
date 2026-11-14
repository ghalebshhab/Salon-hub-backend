package com.salon.backend.Services.Assistant;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds the intent of a message from its words. The result selects which
 * real data the assistant loads, so the reply can never invent salons or jobs.
 */
public final class AssistantIntentDetector {

    private static final Map<AssistantIntent, List<String>> KEYWORDS = new EnumMap<>(AssistantIntent.class);

    static {
        KEYWORDS.put(AssistantIntent.GREETING, List.of(
                "hi", "hello", "hey", "hala", "lamsa", "marhaba", "salam", "مرحبا", "هلا", "السلام"));
        KEYWORDS.put(AssistantIntent.TOP_SALONS, List.of(
                "top", "best", "rated", "rating", "ratings", "highest", "popular", "recommend",
                "recommended", "ranking", "rank", "افضل", "أفضل", "تقييم"));
        KEYWORDS.put(AssistantIntent.FIND_SALONS, List.of(
                "salon", "salons", "barber", "barbers", "near", "haircut", "book", "booking",
                "appointment", "صالون", "صالونات", "حلاق", "حجز"));
        KEYWORDS.put(AssistantIntent.JOBS, List.of(
                "job", "jobs", "career", "opportunity", "opportunities", "vacancy",
                "vacancies", "apply", "position", "positions", "employment", "وظيفة", "وظائف",
                "شغل", "عمل", "فرصة", "فرص"));
        KEYWORDS.put(AssistantIntent.HIRING, List.of(
                "hire", "hiring", "staff", "employee", "employees", "candidate", "candidates",
                "capacity", "seats", "stylists", "recruit", "موظف", "موظفين", "توظيف"));
        KEYWORDS.put(AssistantIntent.APP_HELP, List.of(
                "how", "what", "where", "help", "salonhub", "account", "password", "community",
                "notification", "notifications", "review", "reviews", "cancel", "profile",
                "page", "find", "كيف", "ماذا", "وين", "مساعدة"));
    }

    private AssistantIntentDetector() {
    }

    public static AssistantIntent detect(String message) {
        Set<String> words = words(message);
        if (words.isEmpty()) {
            return AssistantIntent.UNKNOWN;
        }

        Map<AssistantIntent, Integer> scores = new EnumMap<>(AssistantIntent.class);
        KEYWORDS.forEach((intent, keywords) -> scores.put(intent,
                (int) keywords.stream()
                        .map(JordanGovernorate::normalize)
                        .filter(words::contains)
                        .count()));

        boolean mentionsPlace = JordanGovernorate.mentionedIn(message).isPresent();
        int top = scores.get(AssistantIntent.TOP_SALONS);
        int salons = scores.get(AssistantIntent.FIND_SALONS);
        int jobs = scores.get(AssistantIntent.JOBS);
        int hiring = scores.get(AssistantIntent.HIRING);

        // Most specific first: ranking questions, then work, then salons.
        if (top > 0 && jobs == 0 && hiring == 0) {
            return AssistantIntent.TOP_SALONS;
        }
        if (hiring > jobs && hiring > 0) {
            return AssistantIntent.HIRING;
        }
        if (jobs > 0) {
            return AssistantIntent.JOBS;
        }
        boolean asksHow = words.contains("how") || words.contains("كيف");
        if (asksHow && !mentionsPlace) {
            return AssistantIntent.APP_HELP;
        }
        if (salons > 0 || mentionsPlace) {
            return AssistantIntent.FIND_SALONS;
        }
        if (scores.get(AssistantIntent.APP_HELP) > 0) {
            return AssistantIntent.APP_HELP;
        }
        if (scores.get(AssistantIntent.GREETING) > 0 && words.size() <= 4) {
            return AssistantIntent.GREETING;
        }
        return AssistantIntent.UNKNOWN;
    }

    static Set<String> words(String message) {
        if (message == null) {
            return Set.of();
        }
        return Arrays.stream(JordanGovernorate.normalize(message).split(" "))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());
    }
}
