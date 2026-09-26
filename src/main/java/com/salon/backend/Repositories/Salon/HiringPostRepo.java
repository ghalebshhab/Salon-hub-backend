package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HiringPostRepo extends JpaRepository<HiringPost, Long>,
        JpaSpecificationExecutor<HiringPost> {

    List<HiringPost> findAllBySalonAndStatus(
            Salon salon,
            HiringPostStatus status
    );

    List<HiringPost> findAllByStatus(HiringPostStatus status);
}
