package com.salon.backend.Repositories.Media;

import com.salon.backend.Entities.media.EmployeeWorkMedia;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeWorkMediaRepo extends JpaRepository<EmployeeWorkMedia, Long> {
    @Query("""
            select media from EmployeeWorkMedia media
            join fetch media.employee employee
            where media.salon.id = :salonId
              and employee.salon.id = :salonId
              and employee.role = :role
              and employee.status = :status
            order by media.createdAt desc, media.id desc
            """)
    Page<EmployeeWorkMedia> findPublicSalonWork(
            @Param("salonId") Long salonId,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable);
    Optional<EmployeeWorkMedia> findByIdAndEmployeeId(Long id, Long employeeId);
}
