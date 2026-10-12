package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Review.CreateSalonReviewRequest;
import com.salon.backend.DTOs.Salon.Review.SalonReviewResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Salon.Review.SalonReviewManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class SalonReviewController {

    private final SalonReviewManagement salonReviewManagement;

    @PostMapping
    public ApiResponse<SalonReviewResponse> createReview(
            @RequestBody CreateSalonReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonReviewManagement.createReview(request, currentUser.getId());
    }

    @GetMapping("/salon/{salonId}")
    public ApiResponse<List<SalonReviewResponse>> getSalonReviews(@PathVariable Long salonId) {
        return salonReviewManagement.getSalonReviews(salonId);
    }

    @GetMapping("/salon/{salonId}/summary")
    public ApiResponse<SalonReviewSummaryResponse> getSalonReviewSummary(@PathVariable Long salonId) {
        return salonReviewManagement.getSalonReviewSummary(salonId);
    }

    @GetMapping("/my")
    public ApiResponse<List<SalonReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonReviewManagement.getMyReviews(currentUser.getId());
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonReviewManagement.deleteMyReview(reviewId, currentUser.getId());
    }
}
