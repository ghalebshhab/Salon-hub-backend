package com.salon.backend.Repositories.Community;

import com.salon.backend.Entities.community.CommunityPostLike;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityPostLikeRepo extends JpaRepository<CommunityPostLike, Long> {
    Optional<CommunityPostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    List<CommunityPostLike> findByPostIdInAndUserId(Collection<Long> postIds, Long userId);
    List<CommunityPostLike> findByPostIdAndUserStatusOrderByCreatedAtDesc(Long postId, UserStatus status);
    void deleteByPostId(Long postId);
}
