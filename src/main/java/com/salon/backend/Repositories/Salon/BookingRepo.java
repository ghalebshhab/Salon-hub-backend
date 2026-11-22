package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepo extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findBySalonOwnerIdOrderByBookingDateDescBookingTimeDesc(Long ownerId);

    List<Booking> findBySalonOwnerIdAndStatusOrderByBookingDateAscBookingTimeAsc(
            Long ownerId,
            BookingStatus status
    );

    List<Booking> findByAssignedEmployeeIdOrderByBookingDateDescBookingTimeDesc(Long employeeId);

    List<Booking> findByAssignedEmployeeIdAndBookingDateAndStatus(
            Long employeeId,
            LocalDate bookingDate,
            BookingStatus status
    );

    List<Booking> findByAssignedEmployeeIdInAndBookingDateAndStatus(
            Collection<Long> employeeIds,
            LocalDate bookingDate,
            BookingStatus status
    );

    /** Bookings that occupy an employee's time: held requests, confirmed and running ones. */
    List<Booking> findByAssignedEmployeeIdInAndBookingDateAndStatusIn(
            Collection<Long> employeeIds,
            LocalDate bookingDate,
            Collection<BookingStatus> statuses
    );

    List<Booking> findByAssignedEmployeeIdAndBookingDateAndStatusIn(
            Long employeeId,
            LocalDate bookingDate,
            Collection<BookingStatus> statuses
    );

    long countByAssignedEmployeeIdAndBookingDateBetweenAndStatusIn(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<BookingStatus> statuses
    );

    Optional<Booking> findFirstByAssignedEmployeeIdAndStatusOrderByBookingTimeAsc(
            Long employeeId,
            BookingStatus status
    );

    long countByStatus(BookingStatus status);

    List<Booking> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    @Query("SELECT COALESCE(SUM(booking.totalPrice), 0) FROM Booking booking")
    BigDecimal sumGrossBookingValue();

    @Query("SELECT COALESCE(SUM(booking.totalPrice), 0) FROM Booking booking WHERE booking.status = :status")
    BigDecimal sumBookingValueByStatus(@Param("status") BookingStatus status);

    /**
     * Pending requests that passed their answer deadline: older than the
     * response window, or whose appointment start is already reached.
     */
    @Query("""
            SELECT booking.id FROM Booking booking
            WHERE booking.status = :status
              AND (booking.createdAt <= :createdBefore
                   OR booking.bookingDate < :today
                   OR (booking.bookingDate = :today AND booking.bookingTime <= :nowTime))
            """)
    List<Long> findOverduePendingIds(
            @Param("status") BookingStatus status,
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("today") LocalDate today,
            @Param("nowTime") java.time.LocalTime nowTime
    );

    @Query("""
            SELECT booking.id FROM Booking booking
            WHERE booking.status = :status
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
            """)
    List<Long> findIdsByStatusAndDateRange(
            @Param("status") BookingStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    long countBySalonIdAndBookingDateBetweenAndStatusIn(
            Long salonId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<BookingStatus> statuses
    );

    long countBySalonIdAndCustomerIdAndStatus(Long salonId, Long customerId, BookingStatus status);

    /** No-show count per customer at one salon: customer id, count. */
    @Query("""
            SELECT booking.customer.id, COUNT(booking)
            FROM Booking booking
            WHERE booking.salon.id = :salonId
              AND booking.customer.id IN :customerIds
              AND booking.status = :noShowStatus
            GROUP BY booking.customer.id
            """)
    List<Object[]> countNoShowsByCustomer(
            @Param("salonId") Long salonId,
            @Param("customerIds") Collection<Long> customerIds,
            @Param("noShowStatus") BookingStatus noShowStatus
    );

    /** The owner dashboard table and lists: one salon, one appointment period. */
    List<Booking> findBySalonOwnerIdAndBookingDateBetweenOrderByBookingDateDescBookingTimeDesc(
            Long ownerId,
            LocalDate fromDate,
            LocalDate toDate
    );

    /** Owner dashboard: one row per day and status with count and booking value. */
    @Query("""
            SELECT booking.bookingDate, booking.status, COUNT(booking),
                   COALESCE(SUM(booking.totalPrice), 0)
            FROM Booking booking
            WHERE booking.salon.id = :salonId
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
            GROUP BY booking.bookingDate, booking.status
            """)
    List<Object[]> sumByDayAndStatus(
            @Param("salonId") Long salonId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /** Owner dashboard busy hours: one row per day and start hour. */
    @Query("""
            SELECT booking.bookingDate, EXTRACT(HOUR FROM booking.bookingTime), COUNT(booking)
            FROM Booking booking
            WHERE booking.salon.id = :salonId
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
              AND booking.status IN :statuses
            GROUP BY booking.bookingDate, EXTRACT(HOUR FROM booking.bookingTime)
            """)
    List<Object[]> countByDayAndHour(
            @Param("salonId") Long salonId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT COUNT(DISTINCT booking.customer.id)
            FROM Booking booking
            WHERE booking.salon.id = :salonId
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
            """)
    long countCustomers(
            @Param("salonId") Long salonId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /** Customers in the period who already had a completed visit at this salon before it. */
    @Query("""
            SELECT COUNT(DISTINCT booking.customer.id)
            FROM Booking booking
            WHERE booking.salon.id = :salonId
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
              AND EXISTS (
                  SELECT 1 FROM Booking earlier
                  WHERE earlier.salon.id = :salonId
                    AND earlier.customer.id = booking.customer.id
                    AND earlier.status = :completedStatus
                    AND earlier.bookingDate < :fromDate
              )
            """)
    long countReturningCustomers(
            @Param("salonId") Long salonId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("completedStatus") BookingStatus completedStatus
    );

    /** Payment state of the completed visits in the period: status, count, amount, refunded. */
    @Query("""
            SELECT payment.status, COUNT(payment),
                   COALESCE(SUM(payment.amountFils), 0),
                   COALESCE(SUM(payment.refundedFils), 0)
            FROM Booking booking JOIN booking.payment payment
            WHERE booking.salon.id = :salonId
              AND booking.bookingDate BETWEEN :fromDate AND :toDate
              AND booking.status = :completedStatus
            GROUP BY payment.status
            """)
    List<Object[]> sumPaymentsOfCompleted(
            @Param("salonId") Long salonId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("completedStatus") BookingStatus completedStatus
    );

    /** The employee's latest timed services, to learn how fast they work. */
    List<Booking> findTop20ByAssignedEmployeeIdAndStatusAndTrackingServiceStartedAtIsNotNullOrderByCompletedAtDesc(
            Long employeeId,
            BookingStatus status
    );

    /** Today's confirmed appointments whose customer turned on "Leave on time" alerts. */
    @Query("""
            SELECT booking.id FROM Booking booking
            WHERE booking.status = :status
              AND booking.bookingDate = :today
              AND booking.tracking.planLatitude IS NOT NULL
            """)
    List<Long> findDepartureAlertCandidateIds(
            @Param("status") BookingStatus status,
            @Param("today") LocalDate today
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT booking FROM Booking booking WHERE booking.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
