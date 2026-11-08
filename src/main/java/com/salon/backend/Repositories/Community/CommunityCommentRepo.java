package com.salon.backend.Repositories.Community;

import com.salon.backend.Entities.community.CommunityComment;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityCommentRepo extends JpaRepository<CommunityComment, Long> {
    Page<CommunityComment> findByPostIdAndAuthorStatus(Long postId, UserStatus status, Pageable pageable);
    long countByPostIdAndAuthorStatus(Long postId, UserStatus status);
    Optional<CommunityComment> findByIdAndPostId(Long id, Long postId);
    void deleteByPostId(Long postId);
}
