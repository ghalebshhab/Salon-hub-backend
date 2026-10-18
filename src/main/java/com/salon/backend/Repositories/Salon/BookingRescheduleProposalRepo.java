package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingRescheduleProposalRepo
        extends JpaRepository<BookingRescheduleProposal, Long> {

    boolean existsByBookingIdAndStatus(Long bookingId, BookingRescheduleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT proposal
            FROM BookingRescheduleProposal proposal
            JOIN FETCH proposal.booking booking
            WHERE proposal.id = :proposalId
              AND booking.id = :bookingId
            """)
    Optional<BookingRescheduleProposal> findOwnedProposalForUpdate(
            @Param("proposalId") Long proposalId,
            @Param("bookingId") Long bookingId
    );

    @Modifying
    @Query("""
            UPDATE BookingRescheduleProposal proposal
            SET proposal.status = :cancelledStatus,
                proposal.respondedAt = :respondedAt
            WHERE proposal.booking.id = :bookingId
              AND proposal.status = :pendingStatus
            """)
    int cancelPendingForBooking(
            @Param("bookingId") Long bookingId,
            @Param("pendingStatus") BookingRescheduleStatus pendingStatus,
            @Param("cancelledStatus") BookingRescheduleStatus cancelledStatus,
            @Param("respondedAt") LocalDateTime respondedAt
    );
}
