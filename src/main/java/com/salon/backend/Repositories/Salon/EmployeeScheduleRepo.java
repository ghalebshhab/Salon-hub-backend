package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.schedule.EmployeeSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeScheduleRepo extends JpaRepository<EmployeeSchedule, Long> {

    Optional<EmployeeSchedule> findByEmployeeId(Long employeeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT schedule FROM EmployeeSchedule schedule WHERE schedule.employee.id = :employeeId")
    Optional<EmployeeSchedule> findByEmployeeIdForUpdate(@Param("employeeId") Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
