package com.salon.backend.Services.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonOpenningStatus;
import com.salon.backend.Repositories.Salon.SalonClosureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Tells if a salon is open at this moment, in Jordan time.
 * The server can run in any zone (Render runs in UTC), so the
 * salon hours are always compared with the time in Amman.
 */
@Component
@RequiredArgsConstructor
public class SalonOpenStatusResolver {

    public static final ZoneId SALON_ZONE = ZoneId.of("Asia/Amman");

    private final Clock clock;
    private final SalonClosureRepo salonClosureRepo;

    public SalonOpenningStatus resolve(Salon salon) {
        if (salon == null || salon.isBlockedByAdmin() || salon.isBlockedByOwner()) {
            return SalonOpenningStatus.Closed;
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(SALON_ZONE));
        if (!isWithinHours(salon.getOpeningTime(), salon.getClosingTime(), now.toLocalTime())) {
            return SalonOpenningStatus.Closed;
        }
        if (salon.getId() != null && salonClosureRepo.isSalonClosedOn(salon.getId(), now.toLocalDate())) {
            return SalonOpenningStatus.Closed;
        }
        return SalonOpenningStatus.Open;
    }

    static boolean isWithinHours(LocalTime opening, LocalTime closing, LocalTime now) {
        if (opening == null || closing == null || opening.equals(closing)) {
            return false;
        }
        if (opening.isBefore(closing)) {
            return !now.isBefore(opening) && now.isBefore(closing);
        }
        // Hours go past midnight, for example 18:00 to 02:00.
        return !now.isBefore(opening) || now.isBefore(closing);
    }
}
