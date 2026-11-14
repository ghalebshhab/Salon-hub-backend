package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.reviews.SalonReview;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalonReviewRepo extends JpaRepository<SalonReview, Long> {

    boolean existsByBookingId(Long bookingId);

    List<SalonReview> findBySalonIdAndCustomerStatusOrderByCreatedAtDesc(
            Long salonId,
            UserStatus customerStatus
    );

    List<SalonReview> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<SalonReview> findByIdAndCustomerId(Long reviewId, Long customerId);

    @Query("""
            SELECT AVG(review.rating) AS averageRating,
                   COUNT(review) AS totalReviews
            FROM SalonReview review
            WHERE review.salon.id = :salonId
              AND review.customer.status = :customerStatus
            """)
    SalonReviewSummaryProjection summarizeSalonReviews(
            @Param("salonId") Long salonId,
            @Param("customerStatus") UserStatus customerStatus
    );

    @Query("""
            SELECT review.salon.id AS salonId,
                   AVG(review.rating) AS averageRating,
                   COUNT(review) AS totalReviews
            FROM SalonReview review
            WHERE review.customer.status = :customerStatus
            GROUP BY review.salon.id
            """)
    List<SalonRatingProjection> summarizeRatingsBySalon(
            @Param("customerStatus") UserStatus customerStatus
    );
}
