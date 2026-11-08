package com.salon.backend.DTOs.Community;

import java.time.LocalDateTime;

public record CommunityCommentResponse(
        Long id,
        Long postId,
        CommunityAuthorResponse author,
        String content,
        boolean ownedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
