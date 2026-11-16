package com.salon.backend.Repositories.Payment;

import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingPaymentRepo extends JpaRepository<BookingPayment, Long> {

    /** One row per payment method: method, count, amount, refunded, commission. */
    @Query("""
            SELECT payment.method, COUNT(payment),
                   COALESCE(SUM(payment.amountFils), 0),
                   COALESCE(SUM(payment.refundedFils), 0),
                   COALESCE(SUM(payment.commissionFils), 0)
            FROM Booking booking JOIN booking.payment payment
            WHERE booking.salon.id = :salonId
              AND payment.status IN :statuses
              AND payment.paidAt >= :from AND payment.paidAt < :to
            GROUP BY payment.method
            """)
    List<Object[]> sumPaidByMethod(
            @Param("salonId") Long salonId,
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Count and amount of finished appointments that still wait for payment. */
    @Query("""
            SELECT COUNT(payment), COALESCE(SUM(payment.amountFils), 0)
            FROM Booking booking JOIN booking.payment payment
            WHERE booking.salon.id = :salonId
              AND booking.status = :bookingStatus
              AND payment.status = :paymentStatus
            """)
    List<Object[]> sumOutstanding(
            @Param("salonId") Long salonId,
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Query("""
            SELECT booking FROM Booking booking JOIN FETCH booking.payment payment
            WHERE booking.salon.id = :salonId
              AND booking.status = :bookingStatus
              AND payment.status = :paymentStatus
            ORDER BY booking.bookingDate DESC, booking.bookingTime DESC
            """)
    List<Booking> findOutstandingBookings(
            @Param("salonId") Long salonId,
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );
}
