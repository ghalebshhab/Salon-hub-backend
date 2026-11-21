package com.salon.backend.DTOs.Salon.Review;

import java.time.LocalDateTime;

public record SalonReviewResponse(
        Long id,
        Long bookingId,
        Long salonId,
        String salonName,
        Long customerId,
        String customerName,
        /** The reviewer's profile photo, or null. */
        String customerProfilePictureUrl,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
