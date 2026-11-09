package com.salon.backend.DTOs.Community;

import java.time.LocalDateTime;

public record CommunityPostLikeResponse(
        CommunityAuthorResponse user,
        LocalDateTime likedAt
) {}
