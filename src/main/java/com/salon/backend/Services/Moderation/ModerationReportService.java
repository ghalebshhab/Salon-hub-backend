package com.salon.backend.Services.Moderation;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Moderation.*;
import com.salon.backend.Entities.moderation.ReportStatus;
import java.util.List;

public interface ModerationReportService {
    ApiResponse<ModerationReportResponse> createReport(CreateReportRequest request, Long reporterId);
    ApiResponse<List<ModerationReportResponse>> getReports(ReportStatus status, Long adminId);
    ApiResponse<ModerationReportResponse> reviewReport(Long reportId, ReviewReportRequest request, Long adminId);
}
