package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.services.EmployeeServiceCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeeServiceCapabilityRepo
        extends JpaRepository<EmployeeServiceCapability, Long> {

    List<EmployeeServiceCapability> findByEmployeeIdAndServiceSalonIdOrderByServiceNameAsc(
            Long employeeId,
            Long salonId
    );

    List<EmployeeServiceCapability> findByEmployeeIdIn(Collection<Long> employeeIds);

    long countByEmployeeIdAndServiceIdIn(Long employeeId, Collection<Long> serviceIds);

    void deleteByEmployeeId(Long employeeId);
}
