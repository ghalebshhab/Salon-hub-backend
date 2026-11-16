package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.schedule.SalonClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalonClosureRepo extends JpaRepository<SalonClosure, Long> {

    @Query("""
            SELECT COUNT(closure) > 0 FROM SalonClosure closure
            WHERE closure.salon.id = :salonId
              AND closure.startDate <= :date AND closure.endDate >= :date
            """)
    boolean isSalonClosedOn(@Param("salonId") Long salonId, @Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(closure) > 0 FROM SalonClosure closure
            WHERE closure.salon.id = :salonId
              AND closure.startDate <= :endDate AND closure.endDate >= :startDate
            """)
    boolean existsOverlapping(
            @Param("salonId") Long salonId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<SalonClosure> findBySalonIdAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long salonId,
            LocalDate fromDate
    );
}
