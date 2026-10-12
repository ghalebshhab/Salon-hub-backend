package com.salon.backend.DTOs.Salon.Review;

public record CreateSalonReviewRequest(
        Long bookingId,
        Integer rating,
        String comment
) {
}
