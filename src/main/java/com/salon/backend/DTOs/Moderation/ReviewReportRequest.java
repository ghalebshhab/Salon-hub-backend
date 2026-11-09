package com.salon.backend.DTOs.Moderation;

import com.salon.backend.Entities.moderation.ReportStatus;

public record ReviewReportRequest(ReportStatus status, String adminNote) {}
