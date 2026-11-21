package com.salon.backend.Controllers.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardResponse;
import com.salon.backend.DTOs.Owner.OwnerDashboardSummaryResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Owner.OwnerBookingExportService;
import com.salon.backend.Services.Owner.OwnerDashboardService;
import com.salon.backend.Services.Owner.OwnerDashboardSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;
    private final OwnerDashboardSummaryService summaryService;
    private final OwnerBookingExportService exportService;

    /** Salon, team, services, reviews and the bookings of the period (all bookings without dates). */
    @GetMapping
    public ApiResponse<OwnerDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ownerDashboardService.getDashboard(currentUser.getId(), from, to);
    }

    /** Totals counted in the database for the period and the period it is compared with. */
    @GetMapping("/summary")
    public ApiResponse<OwnerDashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareTo,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return summaryService.getSummary(currentUser.getId(), from, to, compareFrom, compareTo);
    }

    /** The bookings of the period as a CSV file (Business plan). Errors come back as JSON. */
    @GetMapping("/export")
    public ResponseEntity<?> exportBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ApiResponse<OwnerBookingExportService.CsvFile> result =
                exportService.exportBookings(currentUser.getId(), from, to);
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }
        OwnerBookingExportService.CsvFile file = result.getData();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(file.content());
    }
}
