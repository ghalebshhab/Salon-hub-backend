package com.salon.backend.Services.Salon;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonOpenningStatus;
import com.salon.backend.Repositories.Salon.SalonClosureRepo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SalonOpenStatusResolverTest {

    private final SalonClosureRepo closureRepo = mock(SalonClosureRepo.class);

    private SalonOpenStatusResolver resolverAtUtc(String isoInstant) {
        Clock utc = Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC);
        return new SalonOpenStatusResolver(utc, closureRepo);
    }

    private Salon salon(String opening, String closing) {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setOpeningTime(LocalTime.parse(opening));
        salon.setClosingTime(LocalTime.parse(closing));
        return salon;
    }

    @Test
    void usesJordanTimeEvenWhenServerRunsInUtc() {
        // 06:30 UTC is 09:30 in Amman, so a 09:00 salon is open.
        SalonOpenStatusResolver resolver = resolverAtUtc("2026-11-17T06:30:00Z");

        assertEquals(SalonOpenningStatus.Open, resolver.resolve(salon("09:00", "23:59")));
    }

    @Test
    void isClosedBeforeOpeningInJordanTime() {
        // 05:30 UTC is 08:30 in Amman.
        SalonOpenStatusResolver resolver = resolverAtUtc("2026-11-17T05:30:00Z");

        assertEquals(SalonOpenningStatus.Closed, resolver.resolve(salon("09:00", "23:59")));
    }

    @Test
    void isClosedOnAClosureDayInJordan() {
        // 22:30 UTC on the 16th is 01:30 on the 17th in Amman.
        when(closureRepo.isSalonClosedOn(eq(1L), eq(LocalDate.of(2026, 11, 17)))).thenReturn(true);
        SalonOpenStatusResolver resolver = resolverAtUtc("2026-11-16T22:30:00Z");

        assertEquals(SalonOpenningStatus.Closed, resolver.resolve(salon("00:00", "23:59")));
    }

    @Test
    void isClosedWhenBlocked() {
        Salon salon = salon("00:00", "23:59");
        salon.setBlockedByOwner(true);

        assertEquals(SalonOpenningStatus.Closed,
                resolverAtUtc("2026-11-17T10:00:00Z").resolve(salon));
        verify(closureRepo, never()).isSalonClosedOn(any(), any());
    }

    @Test
    void supportsHoursThatGoPastMidnight() {
        LocalTime opening = LocalTime.of(18, 0);
        LocalTime closing = LocalTime.of(2, 0);

        assertTrue(SalonOpenStatusResolver.isWithinHours(opening, closing, LocalTime.of(23, 0)));
        assertTrue(SalonOpenStatusResolver.isWithinHours(opening, closing, LocalTime.of(1, 0)));
        assertFalse(SalonOpenStatusResolver.isWithinHours(opening, closing, LocalTime.of(12, 0)));
        assertFalse(SalonOpenStatusResolver.isWithinHours(opening, closing, LocalTime.of(2, 0)));
    }

    @Test
    void isClosedWhenHoursAreMissing() {
        assertFalse(SalonOpenStatusResolver.isWithinHours(null, LocalTime.NOON, LocalTime.NOON));
    }
}
