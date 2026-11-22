package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.services.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonServiceRepo extends JpaRepository<SalonService, Long> {

    List<SalonService> findBySalonIdAndActiveTrueOrderByNameAsc(Long salonId);

    long countBySalonIdAndActiveTrue(Long salonId);

    List<SalonService> findBySalonIdOrderByCreatedAtDesc(Long salonId);

    Optional<SalonService> findByIdAndSalonOwnerId(Long serviceId, Long ownerId);

    List<SalonService> findByIdInAndSalonIdAndActiveTrue(List<Long> ids, Long salonId);

    @Query("select s.id from SalonService s where s.nameAr is null"
            + " or (s.description is not null and s.description <> '' and s.descriptionAr is null)")
    List<Long> findIdsMissingArabic();

    /**
     * Stores Claude's Arabic only into empty fields, and only while the
     * English it was made from is still the saved English.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update SalonService s"
            + " set s.nameAr = coalesce(s.nameAr, :nameAr),"
            + " s.descriptionAr = coalesce(s.descriptionAr, :descriptionAr)"
            + " where s.id = :id and s.name = :name"
            + " and (s.description = :description or (s.description is null and :description is null))")
    int saveArabic(@Param("id") Long id,
                   @Param("name") String name,
                   @Param("description") String description,
                   @Param("nameAr") String nameAr,
                   @Param("descriptionAr") String descriptionAr);
}
