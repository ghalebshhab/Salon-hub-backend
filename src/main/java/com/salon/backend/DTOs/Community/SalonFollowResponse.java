package com.salon.backend.DTOs.Community;

import java.time.LocalDateTime;

public record SalonFollowResponse(CommunitySalonResponse salon, LocalDateTime followedAt) {}
