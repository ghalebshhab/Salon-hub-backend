package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.BookingRescheduleProposalRepo;
import com.salon.backend.Services.Notification.BookingNotificationService;
import com.salon.backend.Services.Payment.BookingPaymentLifecycle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Time-driven booking changes: expiry of unanswered requests and reminders.
 * Each booking is handled in its own transaction, so one failure never blocks
 * the others, and each step is safe to repeat.
 */
@Service
@RequiredArgsConstructor
public class BookingLifecycleService {

    private final BookingRepo bookingRepo;
    private final BookingRescheduleProposalRepo rescheduleProposalRepo;
    private final BookingNotificationService notificationService;
    private final Clock clock;
    private final BookingPaymentLifecycle paymentLifecycle;

    @Transactional(readOnly = true)
    public List<Long> findOverduePendingIds() {
        LocalDateTime now = LocalDateTime.now(clock);
        return bookingRepo.findOverduePendingIds(
                BookingStatus.PENDING,
                now.minus(BookingTimePolicy.RESPONSE_WINDOW),
                now.toLocalDate(),
                now.toLocalTime()
        );
    }

    /** Expires one pending request when its deadline passed. Returns true when it changed. */
    @Transactional
    public boolean expireIfOverdue(Long bookingId) {
        Booking booking = bookingRepo.findByIdForUpdate(bookingId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking == null
                || booking.getStatus() != BookingStatus.PENDING
                || !BookingTimePolicy.isResponseOverdue(booking, now)) {
            return false;
        }
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setDecidedAt(now);
        booking.setUpdatedAt(now);
        rescheduleProposalRepo.cancelPendingForBooking(
                booking.getId(),
                BookingRescheduleStatus.PENDING,
                BookingRescheduleStatus.CANCELLED,
                now
        );
        paymentLifecycle.closeWithoutPayment(booking, "Request expired without an answer", null);
        bookingRepo.save(booking);
        notificationService.bookingExpired(booking);
        return true;
    }

    /** Confirmed appointments that can need a reminder now (today and tomorrow). */
    @Transactional(readOnly = true)
    public List<Long> findReminderCandidateIds() {
        LocalDate today = LocalDate.now(clock);
        return bookingRepo.findIdsByStatusAndDateRange(BookingStatus.ACCEPTED, today, today.plusDays(1));
    }

    /** Sends the reminder that is due for one booking; repeated calls send nothing new. */
    @Transactional
    public BookingTimePolicy.ReminderType sendDueReminder(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking == null || booking.getStatus() != BookingStatus.ACCEPTED) {
            return null;
        }
        BookingTimePolicy.ReminderType type = BookingTimePolicy.dueReminder(booking, now);
        if (type != null) {
            notificationService.bookingReminder(booking, type, now.toLocalDate());
        }
        return type;
    }
}
