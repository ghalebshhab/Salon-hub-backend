package com.salon.backend.DTOs.Community;

import java.util.List;

public record CommunityCommentPageResponse(
        List<CommunityCommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
