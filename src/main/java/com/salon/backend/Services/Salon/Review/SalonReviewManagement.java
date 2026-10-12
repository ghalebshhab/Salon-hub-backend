package com.salon.backend.Services.Salon.Review;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Review.CreateSalonReviewRequest;
import com.salon.backend.DTOs.Salon.Review.SalonReviewResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;

import java.util.List;

public interface SalonReviewManagement {

    ApiResponse<SalonReviewResponse> createReview(CreateSalonReviewRequest request, Long customerId);

    ApiResponse<List<SalonReviewResponse>> getSalonReviews(Long salonId);

    ApiResponse<SalonReviewSummaryResponse> getSalonReviewSummary(Long salonId);

    ApiResponse<List<SalonReviewResponse>> getMyReviews(Long customerId);

    ApiResponse<Void> deleteMyReview(Long reviewId, Long customerId);
}
