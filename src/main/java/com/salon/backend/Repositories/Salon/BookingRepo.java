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

    long countByStatus(BookingStatus status);

    List<Booking> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    @Query("SELECT COALESCE(SUM(booking.totalPrice), 0) FROM Booking booking")
    BigDecimal sumGrossBookingValue();

    @Query("SELECT COALESCE(SUM(booking.totalPrice), 0) FROM Booking booking WHERE booking.status = :status")
    BigDecimal sumBookingValueByStatus(@Param("status") BookingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT booking FROM Booking booking WHERE booking.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
