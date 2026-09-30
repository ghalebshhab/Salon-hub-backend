package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface SalonRepo extends JpaRepository<Salon, Long> {

    Salon findByOwnerPhoneNumber(String ownerPhoneNumber);

    Salon findByOwnerId(Long ownerId);

    Salon findByOwnerEmail(String ownerEmail);

    Salon findByEmail(String email);

    List<Salon> findByStatus(SalonStatus status);

    List<Salon> findByLocation(String location);

    boolean existsBySalonPhoneNumber(String salonPhoneNumber);

    boolean existsByOwnerPhoneNumber(String ownerPhoneNumber);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT salon FROM Salon salon WHERE salon.id = :id")
    Optional<Salon> findByIdForUpdate(@Param("id") Long id);
}
