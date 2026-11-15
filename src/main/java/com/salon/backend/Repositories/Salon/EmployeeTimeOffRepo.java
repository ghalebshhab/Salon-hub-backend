package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.schedule.EmployeeTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeeTimeOffRepo extends JpaRepository<EmployeeTimeOff, Long> {

    @Query("""
            SELECT timeOff.employee.id FROM EmployeeTimeOff timeOff
            WHERE timeOff.employee.id IN :employeeIds
              AND timeOff.startDate <= :date AND timeOff.endDate >= :date
            """)
    List<Long> findEmployeeIdsOffOn(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("date") LocalDate date
    );

    @Query("""
            SELECT COUNT(timeOff) > 0 FROM EmployeeTimeOff timeOff
            WHERE timeOff.employee.id = :employeeId
              AND timeOff.startDate <= :endDate AND timeOff.endDate >= :startDate
            """)
    boolean existsOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<EmployeeTimeOff> findByEmployeeIdAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long employeeId,
            LocalDate fromDate
    );
}
