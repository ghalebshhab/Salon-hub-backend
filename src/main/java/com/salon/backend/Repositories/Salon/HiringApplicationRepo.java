package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.hiringposts.HiringApplication;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface HiringApplicationRepo extends JpaRepository<HiringApplication, Long> {

    boolean existsByHiringPostAndApplicant(
            HiringPost hiringPost,
            User applicant
    );

    List<HiringApplication> findAllByHiringPost(HiringPost hiringPost);

    List<HiringApplication> findAllByApplicantOrderByCreatedAtDesc(User applicant);

    List<HiringApplication> findAllByHiringPostIn(List<HiringPost> hiringPosts);

    long countByHiringPost(HiringPost hiringPost);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT application FROM HiringApplication application WHERE application.id = :id")
    Optional<HiringApplication> findByIdForUpdate(@Param("id") Long id);
}
