package com.salon.backend.Services.Owner;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.PaymentSummaryResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingServiceItemResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Services.Salon.Booking.BookingManagement;
import com.salon.backend.Services.Subscription.PlanLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The owner's bookings of one period as a CSV file for Excel or Google
 * Sheets. Only plans with {@link PlanFeature#CSV_EXPORT} can export, and the
 * period follows the same history limit as the dashboard.
 */
@Service
@RequiredArgsConstructor
public class OwnerBookingExportService {

    /** Excel opens UTF-8 (for example Arabic names) correctly only with this mark. */
    private static final String UTF8_BOM = "﻿";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> HEADER = List.of(
            "Booking ID", "Date", "Time", "Status", "Customer", "Customer phone", "Customer email",
            "Employee", "Services", "Duration (min)", "Total (JOD)", "Payment method",
            "Payment status", "Paid (JOD)", "Receipt", "Booked at");

    private final SalonRepo salonRepo;
    private final BookingManagement bookingManagement;
    private final OwnerDashboardSummaryService summaryService;
    private final PlanLimits planLimits;

    /** A ready file: its name and its bytes. */
    public record CsvFile(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public ApiResponse<CsvFile> exportBookings(Long ownerId, LocalDate from, LocalDate to) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        String planError = planLimits.featureError(salon, PlanFeature.CSV_EXPORT, "CSV export");
        if (planError != null) {
            return ApiResponse.error(planError);
        }
        if (from == null || to == null) {
            return ApiResponse.error("Send both a start and an end date.");
        }
        String periodError = summaryService.periodError(salon, from, to);
        if (periodError != null) {
            return ApiResponse.error(periodError);
        }

        ApiResponse<List<BookingResponse>> bookings = bookingManagement.getMySalonBookings(ownerId, from, to);
        if (!bookings.isSuccess()) {
            return ApiResponse.error(bookings.getMessage());
        }

        StringBuilder csv = new StringBuilder(UTF8_BOM);
        appendRow(csv, HEADER);
        for (BookingResponse booking : bookings.getData()) {
            appendRow(csv, row(booking));
        }
        String fileName = "salonhub-bookings-" + from + "-to-" + to + ".csv";
        return ApiResponse.success("Bookings exported",
                new CsvFile(fileName, csv.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> row(BookingResponse booking) {
        PaymentSummaryResponse payment = booking.payment();
        return List.of(
                text(booking.id()),
                text(booking.bookingDate()),
                booking.bookingTime() == null ? "" : booking.bookingTime().format(TIME),
                text(booking.status()),
                text(booking.customerName()),
                text(booking.customerPhoneNumber()),
                text(booking.customerEmail()),
                text(booking.assignedEmployeeName()),
                services(booking.services()),
                text(booking.totalDurationMinutes()),
                money(booking.totalPrice()),
                payment == null ? "" : text(payment.method()),
                payment == null ? "" : text(payment.status()),
                payment == null || payment.paidAt() == null ? "" : fils(payment.amountFils()),
                payment == null ? "" : text(payment.receiptNumber()),
                booking.createdAt() == null ? "" : booking.createdAt().format(DATE_TIME));
    }

    private static String services(List<BookingServiceItemResponse> items) {
        if (items == null) {
            return "";
        }
        return items.stream()
                .map(BookingServiceItemResponse::name)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining("; "));
    }

    private static String money(BigDecimal value) {
        return value == null ? "" : value.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private static String fils(long amountFils) {
        return BigDecimal.valueOf(amountFils, 3).toPlainString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        csv.append(cells.stream().map(OwnerBookingExportService::cell).collect(Collectors.joining(",")));
        csv.append("\r\n");
    }

    /**
     * Quotes every cell. A cell that starts like a formula gets a leading
     * apostrophe, so a customer name such as "=HYPERLINK(...)" stays text
     * when the owner opens the file in a spreadsheet.
     */
    static String cell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private Salon ownedActiveSalon(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner()
                ? salon
                : null;
    }
}
