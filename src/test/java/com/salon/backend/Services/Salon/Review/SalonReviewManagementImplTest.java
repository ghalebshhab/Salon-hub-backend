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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalonReviewManagementImplTest {

    @Mock
    private SalonReviewRepo salonReviewRepo;

    @Mock
    private BookingRepo bookingRepo;

    @Mock
    private SalonRepo salonRepo;

    @Mock
    private UserRepo userRepo;

    private SalonReviewManagementImpl service;

    @BeforeEach
    void setUp() {
        service = new SalonReviewManagementImpl(
                salonReviewRepo,
                bookingRepo,
                salonRepo,
                userRepo
        );
    }

    @Test
    void createsReviewForOwnedCompletedBooking() {
        User customer = activeUser(1L, "Maya", "Ali");
        Salon salon = activeSalon(2L);
        Booking booking = completedBooking(3L, customer, salon);
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));
        when(salonReviewRepo.existsByBookingId(3L)).thenReturn(false);
        when(salonReviewRepo.save(org.mockito.ArgumentMatchers.any(SalonReview.class)))
                .thenAnswer(invocation -> {
                    SalonReview saved = invocation.getArgument(0);
                    saved.setId(4L);
                    return saved;
                });

        ApiResponse<SalonReviewResponse> response = service.createReview(
                new CreateSalonReviewRequest(3L, 5, "  Excellent service  "),
                1L
        );

        assertTrue(response.isSuccess());
        assertEquals(4L, response.getData().id());
        assertEquals("Excellent service", response.getData().comment());
        assertEquals("Maya Ali", response.getData().customerName());

        ArgumentCaptor<SalonReview> captor = ArgumentCaptor.forClass(SalonReview.class);
        verify(salonReviewRepo).save(captor.capture());
        assertEquals(booking, captor.getValue().getBooking());
        assertEquals(customer, captor.getValue().getCustomer());
    }

    @Test
    void rejectsReviewWhenBookingDoesNotBelongToCustomer() {
        User customer = activeUser(1L, "Maya", "Ali");
        User otherCustomer = activeUser(9L, "Omar", "Saleh");
        Booking booking = completedBooking(3L, otherCustomer, activeSalon(2L));
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        ApiResponse<SalonReviewResponse> response = service.createReview(
                new CreateSalonReviewRequest(3L, 4, null),
                1L
        );

        assertFalse(response.isSuccess());
        assertEquals("Completed booking owned by you was not found", response.getMessage());
        verify(salonReviewRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSecondReviewForTheSameBooking() {
        User customer = activeUser(1L, "Maya", "Ali");
        Booking booking = completedBooking(3L, customer, activeSalon(2L));
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));
        when(salonReviewRepo.existsByBookingId(3L)).thenReturn(true);

        ApiResponse<SalonReviewResponse> response = service.createReview(
                new CreateSalonReviewRequest(3L, 4, null),
                1L
        );

        assertFalse(response.isSuccess());
        assertEquals("This booking has already been reviewed", response.getMessage());
        verify(salonReviewRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsPublicSummaryForActiveCustomerReviews() {
        Salon salon = activeSalon(2L);
        SalonReviewSummaryProjection summary = mock(SalonReviewSummaryProjection.class);
        when(salonRepo.findById(2L)).thenReturn(Optional.of(salon));
        when(summary.getAverageRating()).thenReturn(4.25);
        when(summary.getTotalReviews()).thenReturn(8L);
        when(salonReviewRepo.summarizeSalonReviews(2L, UserStatus.Active))
                .thenReturn(summary);

        ApiResponse<SalonReviewSummaryResponse> response = service.getSalonReviewSummary(2L);

        assertTrue(response.isSuccess());
        assertEquals(4.25, response.getData().averageRating());
        assertEquals(8L, response.getData().totalReviews());
    }

    @Test
    void deletesOnlyAReviewOwnedByTheCurrentCustomer() {
        User customer = activeUser(1L, "Maya", "Ali");
        SalonReview review = new SalonReview();
        review.setId(5L);
        review.setCustomer(customer);
        when(userRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(salonReviewRepo.findByIdAndCustomerId(5L, 1L)).thenReturn(Optional.of(review));

        ApiResponse<Void> response = service.deleteMyReview(5L, 1L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(salonReviewRepo).delete(review);
    }

    private User activeUser(Long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.Active);
        return user;
    }

    private Salon activeSalon(Long id) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName("Glow Salon");
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        return salon;
    }

    private Booking completedBooking(Long id, User customer, Salon salon) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setSalon(salon);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        return booking;
    }
}
