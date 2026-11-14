package com.salon.backend.Repositories.Salon;

/** Average rating and review count of one salon. */
public interface SalonRatingProjection {

    Long getSalonId();

    Double getAverageRating();

    Long getTotalReviews();
}
