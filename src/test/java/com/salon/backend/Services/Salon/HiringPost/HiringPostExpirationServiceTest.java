package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HiringPostExpirationServiceTest {

    @Mock
    private HiringPostRepo hiringPostRepo;

    @InjectMocks
    private HiringPostExpirationService expirationService;

    @Test
    void expiresOpenPostsUsingCurrentTime() {
        expirationService.expireOpenPosts();

        verify(hiringPostRepo).expireOpenPosts(
                any(LocalDateTime.class),
                eq(HiringPostStatus.Open),
                eq(HiringPostStatus.Expired)
        );
    }
}
