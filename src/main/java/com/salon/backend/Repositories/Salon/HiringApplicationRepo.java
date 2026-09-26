package com.salon.backend.Repositories.Salon;

import com.salon.backend.Entities.salons.hiringposts.HiringApplication;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HiringApplicationRepo extends JpaRepository<HiringApplication, Long> {

    boolean existsByHiringPostAndApplicant(
            HiringPost hiringPost,
            User applicant
    );

    List<HiringApplication> findAllByHiringPost(HiringPost hiringPost);
}
