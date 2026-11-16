package com.salon.backend.Services.Payment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.OwnerPaymentSummaryResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.Entities.payments.BookingPayment;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.payments.PaymentStatus;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Payment.BookingPaymentRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Services.Salon.Booking.BookingResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

/** Money overview and the list of finished, unpaid appointments for the salon owner. */
@Service
@RequiredArgsConstructor
public class OwnerPaymentReportService {

    static final int MAX_RANGE_DAYS = 366;
    static final int MAX_PAGE_SIZE = 100;

    private final BookingPaymentRepo paymentRepo;
    private final SalonRepo salonRepo;
    private final BookingResponseMapper responseMapper;
    private final PaymentPolicy paymentPolicy;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ApiResponse<OwnerPaymentSummaryResponse> getSummary(Long ownerId, LocalDate from, LocalDate to) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) {
            return ApiResponse.error("The start date cannot be after the end date");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            return ApiResponse.error("Choose a period of at most one year");
        }

        long cash = 0;
        long card = 0;
        long refunded = 0;
        long commission = 0;
        long count = 0;
        List<Object[]> rows = paymentRepo.sumPaidByMethod(
                salon.getId(),
                EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            long amount = number(row[2]);
            count += number(row[1]);
            refunded += number(row[3]);
            commission += number(row[4]);
            if (method == PaymentMethod.CASH) {
                cash += amount;
            } else {
                card += amount;
            }
        }
        long collected = cash + card;

        List<Object[]> outstandingRows = paymentRepo.sumOutstanding(
                salon.getId(), BookingStatus.COMPLETED, PaymentStatus.UNPAID);
        Object[] outstanding = outstandingRows.isEmpty() ? new Object[]{0L, 0L} : outstandingRows.get(0);

        return ApiResponse.success("Payment summary fetched successfully", new OwnerPaymentSummaryResponse(
                start,
                end,
                collected,
                cash,
                card,
                refunded,
                commission,
                collected - refunded - commission,
                count,
                number(outstanding[1]),
                number(outstanding[0]),
                paymentPolicy.commissionRateBps(),
                BookingPayment.CURRENCY
        ));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getOutstanding(Long ownerId, int page, int size) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("An active, approved salon owned by you is required");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return ApiResponse.error("Page must be 0 or more and size 1 to " + MAX_PAGE_SIZE);
        }
        return ApiResponse.success("Unpaid appointments fetched successfully", responseMapper.map(
                paymentRepo.findOutstandingBookings(
                        salon.getId(),
                        BookingStatus.COMPLETED,
                        PaymentStatus.UNPAID,
                        PageRequest.of(page, size)
                )
        ));
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

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
