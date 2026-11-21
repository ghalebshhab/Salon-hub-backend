package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface EmploymentRepo extends JpaRepository<EmploymentRequest, Long> {
    /** Date of the newest accepted request that put this user in this salon. */
    @Query("""
    SELECT MAX(e.createdAt)
    FROM EmploymentRequest e
    WHERE e.salon.id = :salonId
    AND e.status = com.salon.backend.Entities.salons.employment.EmploymentRequestStatus.Accepted
    AND (e.sender.id = :userId OR e.receiver.id = :userId)
""")
    java.time.LocalDateTime latestAcceptedAt(
            @Param("salonId") Long salonId,
            @Param("userId") Long userId
    );

    boolean existsById(Long employmentRequestId);
    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, EmploymentRequestStatus status);
    boolean existsBySenderAndSalonAndStatus(User sender, Salon salon, EmploymentRequestStatus status);
     List<EmploymentRequest> findAllBySalonAndStatus(Salon salon, EmploymentRequestStatus status);
    List<EmploymentRequest> findAllBySenderAndStatus(User sender, EmploymentRequestStatus status);
    @Query("""
    SELECT e
    FROM EmploymentRequest e
    WHERE e.salon = :salon
    AND e.status = :status
    AND (
        (e.requestType = RequestType.User_Request AND e.sender = :user)
        OR
        (e.requestType = RequestType.Owner_Invite AND e.receiver = :user)
    )
""")
    Optional<EmploymentRequest> findPendingRequest(
            @Param("salon") Salon salon,
            @Param("user") User user,
            @Param("status") EmploymentRequestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM EmploymentRequest request WHERE request.id = :id")
    Optional<EmploymentRequest> findByIdForUpdate(@Param("id") Long id);

    Page<EmploymentRequest> findBySenderIdOrReceiverId(
            Long senderId,
            Long receiverId,
            Pageable pageable
    );

    Page<EmploymentRequest> findBySalonId(Long salonId, Pageable pageable);
}
