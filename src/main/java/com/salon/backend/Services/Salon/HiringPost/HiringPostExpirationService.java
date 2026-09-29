package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HiringPostExpirationService {

    private final HiringPostRepo hiringPostRepo;

    @Scheduled(fixedDelayString = "${salon.hiring-post.expiration-delay-ms:60000}")
    @Transactional
    public void expireOpenPosts() {
        hiringPostRepo.expireOpenPosts(
                LocalDateTime.now(),
                HiringPostStatus.Open,
                HiringPostStatus.Expired
        );
    }
}
