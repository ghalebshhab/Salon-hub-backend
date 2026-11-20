package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import jakarta.persistence.LockModeType;

@Repository
public interface HiringPostRepo extends JpaRepository<HiringPost, Long>,
        JpaSpecificationExecutor<HiringPost> {

    List<HiringPost> findAllBySalonAndStatus(
            Salon salon,
            HiringPostStatus status
    );

    List<HiringPost> findAllByStatus(HiringPostStatus status);

    long countBySalonIdAndStatus(Long salonId, HiringPostStatus status);

    List<HiringPost> findAllBySalonOrderByCreatedAtDesc(Salon salon);

    List<HiringPost> findAllBySalonAndStatusOrderByCreatedAtDesc(
            Salon salon,
            HiringPostStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT post FROM HiringPost post WHERE post.id = :id")
    Optional<HiringPost> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE HiringPost post
            SET post.status = :expiredStatus
            WHERE post.status = :openStatus
            AND post.expiresAt <= :now
            """)
    int expireOpenPosts(
            @Param("now") LocalDateTime now,
            @Param("openStatus") HiringPostStatus openStatus,
            @Param("expiredStatus") HiringPostStatus expiredStatus
    );
}
