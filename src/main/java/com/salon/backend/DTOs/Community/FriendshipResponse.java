package com.salon.backend.DTOs.Community;

import com.salon.backend.Entities.community.FriendshipStatus;
import java.time.LocalDateTime;

public record FriendshipResponse(
        Long id,
        CommunityAuthorResponse user,
        FriendshipStatus status,
        boolean incoming,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {}
