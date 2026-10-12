package com.salon.backend.DTOs.Salon.Review;

public record SalonReviewSummaryResponse(
        double averageRating,
        long totalReviews
) {
}
