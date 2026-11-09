package com.salon.backend.Controllers.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Moderation.*;
import com.salon.backend.Entities.moderation.ReportStatus;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Moderation.ModerationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/reports")
@RequiredArgsConstructor
public class DashboardModerationController {
    private final ModerationReportService reportService;
    @GetMapping
    public ApiResponse<List<ModerationReportResponse>> list(@RequestParam(required = false) ReportStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reportService.getReports(status, currentUser.getId());
    }
    @PutMapping("/{reportId}")
    public ApiResponse<ModerationReportResponse> review(@PathVariable Long reportId, @RequestBody ReviewReportRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reportService.reviewReport(reportId, request, currentUser.getId());
    }
}
