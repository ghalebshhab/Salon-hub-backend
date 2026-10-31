package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.LeaveRequest;
import com.salon.backend.Entities.salons.employment.LeaveRequestStatus;
import com.salon.backend.Entities.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface LeaveRequestRepo
        extends JpaRepository<LeaveRequest, Long> {

    boolean existsByEmployeeAndStatus(
            User employee,
            LeaveRequestStatus status
    );

    Optional<LeaveRequest> findByEmployeeAndStatus(
            User employee,
            LeaveRequestStatus status
    );

    Optional<LeaveRequest> findByEmploymentRequest(
            EmploymentRequest employmentRequest
    );

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByCurrentSalonId(Long salonId, Pageable pageable);
}
