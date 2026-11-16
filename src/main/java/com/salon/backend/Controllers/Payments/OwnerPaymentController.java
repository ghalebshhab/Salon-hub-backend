package com.salon.backend.Controllers.Payments;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.OwnerPaymentSummaryResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Payment.OwnerPaymentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Money overview of the owner's salon. */
@RestController
@RequestMapping("/api/owner/payments")
@RequiredArgsConstructor
public class OwnerPaymentController {

    private final OwnerPaymentReportService reportService;

    @GetMapping("/summary")
    public ApiResponse<OwnerPaymentSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reportService.getSummary(currentUser.getId(), from, to);
    }

    @GetMapping("/outstanding")
    public ApiResponse<List<BookingResponse>> getOutstanding(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return reportService.getOutstanding(currentUser.getId(), page, size);
    }
}
