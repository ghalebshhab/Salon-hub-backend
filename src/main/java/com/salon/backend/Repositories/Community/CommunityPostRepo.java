package com.salon.backend.Repositories.Community;

import com.salon.backend.Entities.community.CommunityPost;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostRepo extends JpaRepository<CommunityPost, Long> {
    Page<CommunityPost> findByAuthorStatus(UserStatus status, Pageable pageable);
    Page<CommunityPost> findByAuthorIdAndAuthorStatus(Long authorId, UserStatus status, Pageable pageable);
    long countByAuthorIdAndAuthorStatus(Long authorId, UserStatus status);
    Optional<CommunityPost> findByIdAndAuthorStatus(Long id, UserStatus status);
}
