package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingAvailabilityResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleAvailabilityRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalRequest;
import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.BookingRescheduleProposalRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingRescheduleManagementImpl implements BookingRescheduleManagement {

    private static final int MAX_NOTE_LENGTH = 1000;

    private final BookingRepo bookingRepo;
    private final BookingRescheduleProposalRepo proposalRepo;
    private final UserRepo userRepo;
    private final BookingAvailabilityService availabilityService;
    private final BookingNotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<BookingAvailabilityResponse> getAvailability(
            Long bookingId,
            BookingRescheduleAvailabilityRequest request,
            Long ownerId) {
        Booking booking = bookingId == null ? null : bookingRepo.findById(bookingId).orElse(null);
        if (!isOwnedActiveBooking(booking, ownerId)) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (!canReschedule(booking)) {
            return ApiResponse.error("Only a pending or accepted future booking can be rescheduled");
        }
        if (request == null || request.bookingDate() == null) {
            return ApiResponse.error("Reschedule date is required");
        }
        return availabilityService.getRescheduleAvailability(booking, request.bookingDate());
    }

    @Override
    @Transactional
    public ApiResponse<BookingRescheduleProposalResponse> propose(
            Long bookingId,
            BookingRescheduleProposalRequest request,
            Long ownerId) {
        Booking booking = bookingId == null
                ? null
                : bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        if (!isOwnedActiveBooking(booking, ownerId)) {
            return ApiResponse.error("Booking for your active salon was not found");
        }
        if (!canReschedule(booking)) {
            return ApiResponse.error("Only a pending or accepted future booking can be rescheduled");
        }

        ApiResponse<Void> validation = validateProposalRequest(request, booking);
        if (!validation.isSuccess()) {
            return ApiResponse.error(validation.getMessage());
        }
        if (proposalRepo.existsByBookingIdAndStatus(
                booking.getId(),
                BookingRescheduleStatus.PENDING
        )) {
            return ApiResponse.error("The customer must decide the current reschedule proposal first");
        }
        if (!isProposedSlotAvailable(booking, request)) {
            return ApiResponse.error("Selected reschedule time is no longer available");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        BookingRescheduleProposal proposal = new BookingRescheduleProposal();
        proposal.setBooking(booking);
        proposal.setProposedBy(booking.getSalon().getOwner());
        proposal.setProposedDate(request.proposedDate());
        proposal.setProposedTime(request.proposedTime());
        proposal.setNote(normalizeText(request.note()));
        proposal.setStatus(BookingRescheduleStatus.PENDING);
        proposal.setCreatedAt(now);
        proposalRepo.save(proposal);
        booking.getRescheduleProposals().add(0, proposal);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);
        notificationService.bookingRescheduleProposed(booking, proposal);

        return ApiResponse.success(
                "Reschedule proposal sent to the customer",
                mapProposal(proposal)
        );
    }

    @Override
    @Transactional
    public ApiResponse<BookingRescheduleProposalResponse> accept(
            Long bookingId,
            Long proposalId,
            Long customerId) {
        BookingRescheduleProposal proposal = findCustomerProposalForUpdate(
                bookingId,
                proposalId,
                customerId
        );
        if (proposal == null) {
            return ApiResponse.error("Reschedule proposal for your booking was not found");
        }
        if (proposal.getStatus() == BookingRescheduleStatus.ACCEPTED) {
            return ApiResponse.success(
                    "Reschedule proposal was already accepted",
                    mapProposal(proposal)
            );
        }
        if (proposal.getStatus() != BookingRescheduleStatus.PENDING) {
            return ApiResponse.error("Only a pending reschedule proposal can be accepted");
        }

        Booking booking = proposal.getBooking();
        if (!canReschedule(booking)) {
            return ApiResponse.error("This booking can no longer be rescheduled");
        }
        BookingRescheduleProposalRequest proposedSlot = new BookingRescheduleProposalRequest(
                proposal.getProposedDate(),
                proposal.getProposedTime(),
                proposal.getNote()
        );
        if (!isProposedSlotAvailable(booking, proposedSlot)) {
            return ApiResponse.error("The proposed appointment time is no longer available");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        booking.setBookingDate(proposal.getProposedDate());
        booking.setBookingTime(proposal.getProposedTime());
        booking.setUpdatedAt(now);
        proposal.setStatus(BookingRescheduleStatus.ACCEPTED);
        proposal.setRespondedAt(now);
        bookingRepo.save(booking);
        proposalRepo.save(proposal);
        notificationService.bookingRescheduleAccepted(booking, proposal);

        return ApiResponse.success("Appointment rescheduled successfully", mapProposal(proposal));
    }

    @Override
    @Transactional
    public ApiResponse<BookingRescheduleProposalResponse> reject(
            Long bookingId,
            Long proposalId,
            Long customerId) {
        BookingRescheduleProposal proposal = findCustomerProposalForUpdate(
                bookingId,
                proposalId,
                customerId
        );
        if (proposal == null) {
            return ApiResponse.error("Reschedule proposal for your booking was not found");
        }
        if (proposal.getStatus() == BookingRescheduleStatus.REJECTED) {
            return ApiResponse.success(
                    "Reschedule proposal was already rejected",
                    mapProposal(proposal)
            );
        }
        if (proposal.getStatus() != BookingRescheduleStatus.PENDING) {
            return ApiResponse.error("Only a pending reschedule proposal can be rejected");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        proposal.setStatus(BookingRescheduleStatus.REJECTED);
        proposal.setRespondedAt(now);
        proposal.getBooking().setUpdatedAt(now);
        proposalRepo.save(proposal);
        bookingRepo.save(proposal.getBooking());
        notificationService.bookingRescheduleRejected(proposal.getBooking(), proposal);

        return ApiResponse.success(
                "Reschedule proposal rejected; the original appointment is unchanged",
                mapProposal(proposal)
        );
    }

    private ApiResponse<Void> validateProposalRequest(
            BookingRescheduleProposalRequest request,
            Booking booking) {
        if (request == null || request.proposedDate() == null || request.proposedTime() == null) {
            return ApiResponse.error("Proposed date and time are required");
        }
        if (request.note() != null && request.note().trim().length() > MAX_NOTE_LENGTH) {
            return ApiResponse.error("Reschedule note cannot exceed 1000 characters");
        }
        if (request.proposedDate().equals(booking.getBookingDate())
                && request.proposedTime().equals(booking.getBookingTime())) {
            return ApiResponse.error("Proposed appointment must differ from the current appointment");
        }
        return ApiResponse.success("Reschedule proposal is valid", null);
    }

    private boolean isProposedSlotAvailable(
            Booking booking,
            BookingRescheduleProposalRequest request) {
        Set<Long> serviceIds = booking.getServiceItems().stream()
                .map(BookingServiceItem::getSourceServiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Long employeeId = booking.getAssignedEmployee() == null
                ? null
                : booking.getAssignedEmployee().getId();
        return availabilityService.isSlotAvailable(
                booking.getSalon(),
                serviceIds,
                request.proposedDate(),
                request.proposedTime(),
                booking.getTotalDurationMinutes(),
                employeeId,
                booking.getId()
        );
    }

    private BookingRescheduleProposal findCustomerProposalForUpdate(
            Long bookingId,
            Long proposalId,
            Long customerId) {
        if (bookingId == null || proposalId == null || customerId == null) {
            return null;
        }
        User customer = userRepo.findById(customerId)
                .filter(user -> user.getStatus() == UserStatus.Active)
                .orElse(null);
        if (customer == null) {
            return null;
        }
        BookingRescheduleProposal proposal = proposalRepo
                .findOwnedProposalForUpdate(proposalId, bookingId)
                .orElse(null);
        Booking lockedBooking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        if (proposal == null
                || lockedBooking == null
                || lockedBooking.getCustomer() == null
                || !customerId.equals(lockedBooking.getCustomer().getId())) {
            return null;
        }
        proposal.setBooking(lockedBooking);
        return proposal;
    }

    private boolean isOwnedActiveBooking(Booking booking, Long ownerId) {
        if (booking == null || ownerId == null || booking.getSalon() == null) {
            return false;
        }
        Salon salon = booking.getSalon();
        return salon.getOwner() != null
                && ownerId.equals(salon.getOwner().getId())
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner()
                && salon.getOwner().getStatus() == UserStatus.Active;
    }

    private boolean canReschedule(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            return false;
        }
        return LocalDateTime.of(booking.getBookingDate(), booking.getBookingTime())
                .isAfter(LocalDateTime.now(clock));
    }

    private BookingRescheduleProposalResponse mapProposal(BookingRescheduleProposal proposal) {
        return new BookingRescheduleProposalResponse(
                proposal.getId(),
                proposal.getProposedDate(),
                proposal.getProposedTime(),
                proposal.getNote(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getRespondedAt()
        );
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
