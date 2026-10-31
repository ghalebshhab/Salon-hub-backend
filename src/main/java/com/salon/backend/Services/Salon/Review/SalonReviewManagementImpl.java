package com.salon.backend.Services.Salon.Review;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Review.CreateSalonReviewRequest;
import com.salon.backend.DTOs.Salon.Review.SalonReviewResponse;
import com.salon.backend.DTOs.Salon.Review.SalonReviewSummaryResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.reviews.SalonReview;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonReviewRepo;
import com.salon.backend.Repositories.Salon.SalonReviewSummaryProjection;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonReviewManagementImpl implements SalonReviewManagement {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final SalonReviewRepo salonReviewRepo;
    private final BookingRepo bookingRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;

    @Override
    @Transactional
    public ApiResponse<SalonReviewResponse> createReview(
            CreateSalonReviewRequest request,
            Long customerId) {

        if (request == null || request.bookingId() == null) {
            return ApiResponse.error("Booking id is required");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            return ApiResponse.error("Rating must be between 1 and 5");
        }

        String comment = normalizeComment(request.comment());
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            return ApiResponse.error("Review comment cannot exceed 2000 characters");
        }

        User customer = findActiveUser(customerId);
        if (customer == null) {
            return ApiResponse.error("Active customer not found");
        }

        Booking booking = bookingRepo.findByIdForUpdate(request.bookingId()).orElse(null);
        if (booking == null || booking.getCustomer().getId() != customerId) {
            return ApiResponse.error("Completed booking owned by you was not found");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            return ApiResponse.error("Only a completed booking can be reviewed");
        }
        if (!isActiveSalon(booking.getSalon())) {
            return ApiResponse.error("The salon is not currently available for reviews");
        }
        if (salonReviewRepo.existsByBookingId(booking.getId())) {
            return ApiResponse.error("This booking has already been reviewed");
        }

        SalonReview review = new SalonReview();
        review.setBooking(booking);
        review.setSalon(booking.getSalon());
        review.setCustomer(customer);
        review.setRating(request.rating());
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        salonReviewRepo.save(review);

        return ApiResponse.success("Review added successfully", mapToResponse(review));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonReviewResponse>> getSalonReviews(Long salonId) {
        Salon salon = findActiveSalon(salonId);
        if (salon == null) {
            return ApiResponse.error("Active salon not found");
        }

        List<SalonReviewResponse> reviews = salonReviewRepo
                .findBySalonIdAndCustomerStatusOrderByCreatedAtDesc(salonId, UserStatus.Active)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ApiResponse.success("Salon reviews fetched successfully", reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<SalonReviewSummaryResponse> getSalonReviewSummary(Long salonId) {
        Salon salon = findActiveSalon(salonId);
        if (salon == null) {
            return ApiResponse.error("Active salon not found");
        }

        SalonReviewSummaryProjection summary =
                salonReviewRepo.summarizeSalonReviews(salonId, UserStatus.Active);
        double averageRating = summary == null || summary.getAverageRating() == null
                ? 0.0
                : summary.getAverageRating();
        long totalReviews = summary == null || summary.getTotalReviews() == null
                ? 0L
                : summary.getTotalReviews();

        return ApiResponse.success(
                "Salon review summary fetched successfully",
                new SalonReviewSummaryResponse(averageRating, totalReviews)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonReviewResponse>> getMyReviews(Long customerId) {
        if (findActiveUser(customerId) == null) {
            return ApiResponse.error("Active customer not found");
        }

        List<SalonReviewResponse> reviews = salonReviewRepo
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ApiResponse.success("Your reviews fetched successfully", reviews);
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteMyReview(Long reviewId, Long customerId) {
        if (findActiveUser(customerId) == null) {
            return ApiResponse.error("Active customer not found");
        }

        SalonReview review = reviewId == null
                ? null
                : salonReviewRepo.findByIdAndCustomerId(reviewId, customerId).orElse(null);
        if (review == null) {
            return ApiResponse.error("Review owned by you was not found");
        }

        salonReviewRepo.delete(review);
        return ApiResponse.success("Review deleted successfully", null);
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepo.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.Active)
                .orElse(null);
    }

    private Salon findActiveSalon(Long salonId) {
        if (salonId == null) {
            return null;
        }
        return salonRepo.findById(salonId)
                .filter(this::isActiveSalon)
                .orElse(null);
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private SalonReviewResponse mapToResponse(SalonReview review) {
        User customer = review.getCustomer();
        return new SalonReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getSalon().getId(),
                review.getSalon().getName(),
                customer.getId(),
                customer.getFirstName() + " " + customer.getLastName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
