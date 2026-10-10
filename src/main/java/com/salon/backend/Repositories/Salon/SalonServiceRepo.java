package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.services.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonServiceRepo extends JpaRepository<SalonService, Long> {

    List<SalonService> findBySalonIdAndActiveTrueOrderByNameAsc(Long salonId);

    List<SalonService> findBySalonIdOrderByCreatedAtDesc(Long salonId);

    Optional<SalonService> findByIdAndSalonOwnerId(Long serviceId, Long ownerId);
}
