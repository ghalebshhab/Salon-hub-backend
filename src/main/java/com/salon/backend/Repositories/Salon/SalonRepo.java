package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface SalonRepo extends JpaRepository<Salon, Long> {

    Salon findByOwnerPhoneNumber(String ownerPhoneNumber);

    Salon findByOwnerId(Long ownerId);

    Salon findByEmail(String email);

    List<Salon> findByStatus(SalonStatus status);

    long countByStatus(SalonStatus status);

    @Query("SELECT COUNT(salon) FROM Salon salon WHERE salon.blockedByAdmin = true OR salon.blockedByOwner = true")
    long countBlockedSalons();

    List<Salon> findByLocation(String location);

    boolean existsBySalonPhoneNumber(String salonPhoneNumber);

    boolean existsByOwnerPhoneNumber(String ownerPhoneNumber);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT salon FROM Salon salon WHERE salon.id = :id")
    Optional<Salon> findByIdForUpdate(@Param("id") Long id);

    @Query("select s.id from Salon s where s.description is not null"
            + " and s.description <> '' and s.descriptionAr is null")
    List<Long> findIdsMissingArabicDescription();

    /**
     * Stores Claude's Arabic only when the field is empty and the English it
     * was made from is still the saved English. A bulk update does not change
     * the version, so it never makes an owner's save fail.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Salon s set s.descriptionAr = :descriptionAr"
            + " where s.id = :id and s.description = :description and s.descriptionAr is null")
    int saveArabicDescription(@Param("id") Long id,
                              @Param("description") String description,
                              @Param("descriptionAr") String descriptionAr);
}
