package com.salon.backend.DTOs.Community;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long id,
        CommunityAuthorResponse author,
        CommunitySalonResponse salon,
        String content,
        String mediaUrl,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        boolean ownedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
