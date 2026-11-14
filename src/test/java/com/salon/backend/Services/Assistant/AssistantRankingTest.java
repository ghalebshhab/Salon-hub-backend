package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantRankingTest {

    private static AssistantSalonCard salon(Double rating, long reviews) {
        return new AssistantSalonCard(1L, "Salon", "Amman", "Amman", null, rating, reviews, true, null);
    }

    @Test
    void manyGoodReviewsBeatOnePerfectReview() {
        double mean = 4.0;
        assertTrue(AssistantDataService.weightedRating(salon(4.8, 40), mean)
                > AssistantDataService.weightedRating(salon(5.0, 1), mean));
    }

    @Test
    void salonsWithoutReviewsRankLast() {
        assertEquals(0, AssistantDataService.weightedRating(salon(null, 0), 4.0));
    }

    @Test
    void moreReviewsMoveTheScoreTowardTheSalonAverage() {
        double few = AssistantDataService.weightedRating(salon(5.0, 2), 3.0);
        double many = AssistantDataService.weightedRating(salon(5.0, 50), 3.0);
        assertTrue(many > few);
        assertTrue(many < 5.0);
    }
}
