package com.salon.backend.DTOs.Moderation;

import com.salon.backend.Entities.moderation.ReportReason;
import com.salon.backend.Entities.moderation.ReportTargetType;

public record CreateReportRequest(ReportTargetType targetType, Long targetId, ReportReason reason, String details) {}
