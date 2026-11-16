package com.salon.backend.Services.Salon.Closure;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureRequest;
import com.salon.backend.DTOs.Salon.Closure.SalonClosureResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.schedule.SalonClosure;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonClosureRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalonClosureManagementImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T07:00:00Z"), ZoneId.of("Asia/Amman"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    @Mock private SalonClosureRepo closureRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private BookingRepo bookingRepo;

    private SalonClosureManagementImpl service;
    private Salon salon;

    @BeforeEach
    void setUp() {
        service = new SalonClosureManagementImpl(closureRepo, salonRepo, bookingRepo, CLOCK);
        salon = new Salon();
        salon.setId(10L);
        salon.setStatus(SalonStatus.Accepted);
    }

    @Test
    void theOwnerSavesAClosureAndSeesTheAppointmentsInsideIt() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(bookingRepo.countBySalonIdAndBookingDateBetweenAndStatusIn(any(), any(), any(), any())).thenReturn(3L);

        ApiResponse<SalonClosureResponse> response = service.addClosure(
                10L, new SalonClosureRequest(TODAY.plusDays(5), TODAY.plusDays(7), "  Eid holiday "), 2L);

        assertTrue(response.isSuccess());
        assertEquals(3, response.getData().affectedBookings());
        assertEquals("Eid holiday", response.getData().reason());
        verify(closureRepo).save(any());
    }

    @Test
    void anotherSalonsOwnerCannotAddAClosure() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);

        ApiResponse<SalonClosureResponse> response = service.addClosure(
                11L, new SalonClosureRequest(TODAY, TODAY, null), 2L);

        assertFalse(response.isSuccess());
        verify(closureRepo, never()).save(any());
    }

    @Test
    void rejectsPastLongAndOverlappingClosures() {
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);

        assertFalse(service.addClosure(10L,
                new SalonClosureRequest(TODAY.minusDays(3), TODAY.minusDays(1), null), 2L).isSuccess());
        assertFalse(service.addClosure(10L,
                new SalonClosureRequest(TODAY.plusDays(2), TODAY.plusDays(1), null), 2L).isSuccess());
        assertFalse(service.addClosure(10L,
                new SalonClosureRequest(TODAY, TODAY.plusDays(60), null), 2L).isSuccess());
        assertFalse(service.addClosure(10L,
                new SalonClosureRequest(TODAY.plusDays(400), TODAY.plusDays(401), null), 2L).isSuccess());

        when(closureRepo.existsOverlapping(anyLong(), any(), any())).thenReturn(true);
        assertFalse(service.addClosure(10L,
                new SalonClosureRequest(TODAY, TODAY, null), 2L).isSuccess());

        verify(closureRepo, never()).save(any());
    }

    @Test
    void anyoneCanSeeUpcomingClosuresWithoutBookingCounts() {
        when(salonRepo.findById(10L)).thenReturn(Optional.of(salon));
        when(closureRepo.findBySalonIdAndEndDateGreaterThanEqualOrderByStartDateAsc(10L, TODAY))
                .thenReturn(List.of(closure(5L, salon)));

        ApiResponse<List<SalonClosureResponse>> response = service.getUpcomingClosures(10L);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertNull(response.getData().get(0).affectedBookings());
    }

    @Test
    void theOwnerRemovesOnlyAClosureOfTheirSalon() {
        Salon other = new Salon();
        other.setId(11L);
        when(salonRepo.findByOwnerId(2L)).thenReturn(salon);
        when(closureRepo.findById(5L)).thenReturn(Optional.of(closure(5L, salon)));
        when(closureRepo.findById(6L)).thenReturn(Optional.of(closure(6L, other)));

        assertTrue(service.removeClosure(10L, 5L, 2L).isSuccess());
        assertFalse(service.removeClosure(10L, 6L, 2L).isSuccess());
        verify(closureRepo).delete(any());
    }

    private SalonClosure closure(Long id, Salon owner) {
        SalonClosure closure = new SalonClosure();
        closure.setId(id);
        closure.setSalon(owner);
        closure.setStartDate(TODAY.plusDays(1));
        closure.setEndDate(TODAY.plusDays(2));
        return closure;
    }
}
