package com.salon.backend.Repositories.Community;

import com.salon.backend.Entities.community.SalonFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalonFollowRepo extends JpaRepository<SalonFollow, Long> {
    Optional<SalonFollow> findByUserIdAndSalonId(Long userId, Long salonId);
    List<SalonFollow> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
