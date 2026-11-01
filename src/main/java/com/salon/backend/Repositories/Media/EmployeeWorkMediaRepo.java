package com.salon.backend.Repositories.Media;

import com.salon.backend.Entities.media.EmployeeWorkMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeWorkMediaRepo extends JpaRepository<EmployeeWorkMedia, Long> {
    Page<EmployeeWorkMedia> findBySalonIdOrderByCreatedAtDesc(Long salonId, Pageable pageable);
    Optional<EmployeeWorkMedia> findByIdAndEmployeeId(Long id, Long employeeId);
}
