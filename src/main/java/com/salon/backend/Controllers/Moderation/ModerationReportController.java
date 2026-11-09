package com.salon.backend.Controllers.Moderation;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Moderation.*;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Moderation.ModerationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ModerationReportController {
    private final ModerationReportService reportService;
    @PostMapping
    public ApiResponse<ModerationReportResponse> create(@RequestBody CreateReportRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reportService.createReport(request, currentUser.getId());
    }
}
