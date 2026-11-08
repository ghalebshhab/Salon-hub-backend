package com.salon.backend.Repositories.Community;

import com.salon.backend.Entities.community.Friendship;
import com.salon.backend.Entities.community.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepo extends JpaRepository<Friendship, Long> {
    Optional<Friendship> findByLowerUserIdAndHigherUserId(Long lowerUserId, Long higherUserId);
    List<Friendship> findByAddresseeIdAndStatusOrderByCreatedAtDesc(Long addresseeId, FriendshipStatus status);
    List<Friendship> findByRequesterIdAndStatusOrAddresseeIdAndStatusOrderByRespondedAtDesc(
            Long requesterId, FriendshipStatus requesterStatus, Long addresseeId, FriendshipStatus addresseeStatus);
}
