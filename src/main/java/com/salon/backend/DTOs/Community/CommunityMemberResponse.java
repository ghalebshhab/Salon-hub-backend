package com.salon.backend.DTOs.Community;

public record CommunityMemberResponse(
        CommunityAuthorResponse user,
        Long friendshipId,
        String relationship,
        boolean incomingRequest
) {}
