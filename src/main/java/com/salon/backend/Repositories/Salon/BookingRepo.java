package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT booking FROM Booking booking WHERE booking.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
