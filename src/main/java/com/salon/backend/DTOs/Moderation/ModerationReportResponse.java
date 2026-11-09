package com.salon.backend.DTOs.Moderation;

import com.salon.backend.DTOs.Community.CommunityAuthorResponse;
import com.salon.backend.Entities.moderation.*;
import java.time.LocalDateTime;

public record ModerationReportResponse(Long id, CommunityAuthorResponse reporter, ReportTargetType targetType,
        Long targetId, String targetLabel, ReportReason reason, String details, ReportStatus status,
        String adminNote, CommunityAuthorResponse reviewedBy, LocalDateTime createdAt, LocalDateTime reviewedAt) {}
