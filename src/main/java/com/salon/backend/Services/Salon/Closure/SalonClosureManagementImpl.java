package com.salon.backend.Services.Salon.Closure;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureRequest;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.schedule.SalonClosure;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonClosureRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalonClosureManagementImpl implements SalonClosureManagement {

    static final int MAX_DAYS = 60;
    static final int MAX_DAYS_AHEAD = 365;
    private static final int MAX_REASON_LENGTH = 255;
    private static final Set<BookingStatus> AFFECTED_STATUSES =
            Set.of(BookingStatus.PENDING, BookingStatus.ACCEPTED);

    private final SalonClosureRepo closureRepo;
    private final SalonRepo salonRepo;
    private final BookingRepo bookingRepo;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonClosureResponse>> getUpcomingClosures(Long salonId) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        if (!isActive(salon)) {
            return ApiResponse.error("Active salon not found");
        }
        List<SalonClosureResponse> items = closureRepo
                .findBySalonIdAndEndDateGreaterThanEqualOrderByStartDateAsc(salon.getId(), LocalDate.now(clock))
                .stream()
                .map(closure -> map(closure, null))
                .toList();
        return ApiResponse.success("Salon closures fetched successfully", items);
    }

    @Override
    @Transactional
    public ApiResponse<SalonClosureResponse> addClosure(
            Long salonId,
            SalonClosureRequest request,
            Long ownerId) {
        Salon salon = findOwnedActiveSalon(salonId, ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        String error = validate(request);
        if (error != null) {
            return ApiResponse.error(error);
        }
        if (closureRepo.existsOverlapping(salon.getId(), request.startDate(), request.endDate())) {
            return ApiResponse.error("These dates overlap another closure of the salon");
        }

        SalonClosure closure = new SalonClosure();
        closure.setSalon(salon);
        closure.setStartDate(request.startDate());
        closure.setEndDate(request.endDate());
        closure.setReason(normalize(request.reason()));
        closure.setCreatedAt(LocalDateTime.now(clock));
        closureRepo.save(closure);

        int affected = (int) bookingRepo.countBySalonIdAndBookingDateBetweenAndStatusIn(
                salon.getId(),
                request.startDate(),
                request.endDate(),
                AFFECTED_STATUSES
        );
        String message = affected == 0
                ? "Closure saved"
                : "Closure saved. " + affected + " appointment(s) fall inside it; move or cancel them";
        return ApiResponse.success(message, map(closure, affected));
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeClosure(Long salonId, Long closureId, Long ownerId) {
        Salon salon = findOwnedActiveSalon(salonId, ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        SalonClosure closure = closureId == null ? null : closureRepo.findById(closureId).orElse(null);
        if (closure == null || !closure.getSalon().getId().equals(salon.getId())) {
            return ApiResponse.error("Closure of your salon was not found");
        }
        closureRepo.delete(closure);
        return ApiResponse.success("Closure removed", null);
    }

    private String validate(SalonClosureRequest request) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return "Start date and end date are required";
        }
        LocalDate today = LocalDate.now(clock);
        if (request.endDate().isBefore(request.startDate())) {
            return "End date cannot be before start date";
        }
        if (request.endDate().isBefore(today)) {
            return "A closure must include today or a future date";
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1 > MAX_DAYS) {
            return "A closure cannot be longer than " + MAX_DAYS + " days";
        }
        if (request.startDate().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            return "A closure can be planned at most one year ahead";
        }
        String reason = normalize(request.reason());
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            return "Reason cannot exceed " + MAX_REASON_LENGTH + " characters";
        }
        return null;
    }

    private Salon findOwnedActiveSalon(Long salonId, Long ownerId) {
        if (salonId == null || ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return isActive(salon) && salon.getId().equals(salonId) ? salon : null;
    }

    private boolean isActive(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SalonClosureResponse map(SalonClosure closure, Integer affectedBookings) {
        return new SalonClosureResponse(
                closure.getId(),
                closure.getSalon().getId(),
                closure.getStartDate(),
                closure.getEndDate(),
                closure.getReason(),
                affectedBookings
        );
    }
}
