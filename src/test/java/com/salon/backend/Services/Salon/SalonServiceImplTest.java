package com.salon.backend.Services.Salon;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.DTOs.Salon.CreateSalonRequest;
import com.salon.backend.DTOs.Salon.UpdateSalonLocationRequest;
import com.salon.backend.DTOs.Salon.UpdateSalonRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import com.salon.backend.Services.Notification.SalonNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceImplTest {

    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private SalonNotificationService salonNotificationService;
    @InjectMocks private SalonServiceImpl service;

    @Test
    void publicSalonDetailsOnlyReturnApprovedSalons() {
        Salon accepted = new Salon();
        accepted.setId(12L);
        accepted.setStatus(SalonStatus.Accepted);
        when(salonRepo.findById(accepted.getId())).thenReturn(Optional.of(accepted));

        ApiResponse<SalonResponse> response = service.getSalonById(accepted.getId());

        assertTrue(response.isSuccess());
        assertEquals(accepted.getId(), response.getData().id());
    }

    @Test
    void publicSalonDetailsHideSalonsThatAreNotApproved() {
        Salon rejected = new Salon();
        rejected.setId(12L);
        rejected.setStatus(SalonStatus.Rejected);
        when(salonRepo.findById(rejected.getId())).thenReturn(Optional.of(rejected));

        ApiResponse<SalonResponse> response = service.getSalonById(rejected.getId());

        assertFalse(response.isSuccess());
        assertEquals("Salon is not available", response.getMessage());
    }

    @Test
    void activeSalonsAreQueriedByStatusAndMappedWithoutEntityGraphs() {
        User owner = new User();
        owner.setId(4L);
        owner.setUserName("owner");
        owner.setPassword("must-not-be-returned");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setName("Studio");
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);

        when(salonRepo.findByStatus(SalonStatus.Accepted)).thenReturn(List.of(salon));

        ApiResponse<List<SalonResponse>> response = service.getActiveSalons();

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(salon.getId(), response.getData().get(0).id());
        assertEquals(owner.getId(), response.getData().get(0).ownerId());
        verify(salonRepo).findByStatus(SalonStatus.Accepted);
        verify(salonRepo, never()).findAll();
    }

    @Test
    void rejectedSalonCanBeResubmittedForTheThirdAndFinalTime() {
        User owner = new User();
        owner.setId(4L);
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Rejected);
        salon.setRejectionReason("Fix the salon details");
        salon.setResubmissionCount(2);

        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<SalonResponse> response =
                service.resubmitRejectedSalon(salon.getId(), owner.getId());

        assertTrue(response.isSuccess());
        assertEquals(SalonStatus.Requested, salon.getStatus());
        assertEquals(3, salon.getResubmissionCount());
        assertEquals(0, response.getData().remainingResubmissions());
        assertNull(salon.getRejectionReason());
        verify(salonRepo).save(salon);
        verify(salonNotificationService).notifyRequestResubmitted(salon);
    }

    @Test
    void rejectedSalonCannotExceedThreeResubmissions() {
        User owner = new User();
        owner.setId(4L);
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Rejected);
        salon.setResubmissionCount(3);

        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<SalonResponse> response =
                service.resubmitRejectedSalon(salon.getId(), owner.getId());

        assertFalse(response.isSuccess());
        assertEquals(SalonStatus.Rejected, salon.getStatus());
        verify(salonRepo, never()).save(any());
    }

    @Test
    void allSalonsAreNotQueriedWhenAdminAuthorizationFails() {
        when(adminAuthorizationService.authorize(6L))
                .thenReturn(ApiResponse.error("You are not Admin , Access Denied"));

        ApiResponse<List<SalonResponse>> response = service.getAllSalons(6L);

        assertFalse(response.isSuccess());
        assertEquals("You are not Admin , Access Denied", response.getMessage());
        verifyNoInteractions(salonRepo);
    }

    @Test
    void ownerCanDeleteTheirSalon() {
        User owner = new User();
        owner.setId(4L);
        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        when(salonRepo.findById(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<Void> response = service.deleteSalon(salon.getId(), owner.getId());

        assertTrue(response.isSuccess());
        assertEquals(SalonStatus.DELETED, salon.getStatus());
        verify(salonRepo).save(salon);
    }

    @Test
    void userCannotDeleteAnotherOwnersSalon() {
        User owner = new User();
        owner.setId(4L);
        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        when(salonRepo.findById(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<Void> response = service.deleteSalon(salon.getId(), 99L);

        assertFalse(response.isSuccess());
        verify(salonRepo, never()).save(any());
    }

    @Test
    void salonCreationRejectsMapCoordinatesOutsideJordan() {
        User owner = new User();
        owner.setId(4L);
        owner.setRole(UserRole.USER);
        owner.setPhoneNumber("0791111111");
        CreateSalonRequest request = new CreateSalonRequest();
        request.setSalonName("Plum Studio");
        request.setSalonDescription("A complete salon description");
        request.setSalonPhoneNumber("0792222222");
        request.setSalonEmail("studio@example.com");
        request.setSalonCity("Amman");
        request.setMaxNumberOfEmployees(5L);
        request.setOpeningTime(LocalTime.of(9, 0));
        request.setClosingTime(LocalTime.of(18, 0));
        request.setGoogleMapsUrl("https://www.google.com/maps/@25.2048,55.2708,17z");
        request.setLatitude(25.2048);
        request.setLongitude(55.2708);
        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByOwnerId(owner.getId())).thenReturn(null);

        ApiResponse<?> response = service.createSalon(request, owner.getId());

        assertFalse(response.isSuccess());
        assertEquals("The selected salon location must be inside Jordan", response.getMessage());
        verify(salonRepo, never()).save(any());
    }

    /** Salon saved before the description rule: "hair style" has exactly 10 characters. */
    private Salon legacySalon(User owner) {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setOwner(owner);
        salon.setName("ghaleb shhab");
        salon.setDescription("hair style");
        salon.setEmail("salon@example.com");
        salon.setSalonPhoneNumber("0791234567");
        salon.setLocation("Amman");
        salon.setOpeningTime(LocalTime.of(9, 0));
        salon.setClosingTime(LocalTime.of(18, 0));
        salon.setMaxEmployeesNumber(3L);
        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findById(1L)).thenReturn(Optional.of(salon));
        return salon;
    }

    private UpdateSalonRequest updateFrom(Salon salon) {
        UpdateSalonRequest request = new UpdateSalonRequest();
        request.setSalonName(salon.getName());
        request.setSalonDescription(salon.getDescription());
        request.setSalonEmail(salon.getEmail());
        request.setSalonPhoneNumber(salon.getSalonPhoneNumber());
        request.setSalonCity(salon.getLocation());
        request.setOpeningTime(salon.getOpeningTime());
        request.setClosingTime(salon.getClosingTime());
        request.setMaxNumberOfEmployees(salon.getMaxEmployeesNumber());
        return request;
    }

    @Test
    void changingHoursKeepsAnUnchangedShortDescription() {
        User owner = new User();
        owner.setId(14L);
        owner.setUserName("owner");
        Salon salon = legacySalon(owner);
        UpdateSalonRequest request = updateFrom(salon);
        request.setOpeningTime(LocalTime.of(10, 0));
        request.setClosingTime(LocalTime.of(20, 0));

        ApiResponse<?> response = service.updateSalon(request, 1L, 14L);

        assertTrue(response.isSuccess(), response.getMessage());
        assertEquals(LocalTime.of(10, 0), salon.getOpeningTime());
        assertEquals(LocalTime.of(20, 0), salon.getClosingTime());
        assertEquals("hair style", salon.getDescription());
        verify(salonRepo).save(salon);
    }

    @Test
    void aNewShortDescriptionIsStillRejectedAndNothingChanges() {
        User owner = new User();
        owner.setId(14L);
        Salon salon = legacySalon(owner);
        UpdateSalonRequest request = updateFrom(salon);
        request.setSalonDescription("short");
        request.setOpeningTime(LocalTime.of(10, 0));

        ApiResponse<?> response = service.updateSalon(request, 1L, 14L);

        assertFalse(response.isSuccess());
        assertEquals("Salon Description is Required and must be more than 10 characters", response.getMessage());
        assertEquals(LocalTime.of(9, 0), salon.getOpeningTime());
        verify(salonRepo, never()).save(any());
    }

    @Test
    void newHoursAreComparedWithEachOtherNotWithTheOldClosingTime() {
        User owner = new User();
        owner.setId(14L);
        owner.setUserName("owner");
        Salon salon = legacySalon(owner);
        UpdateSalonRequest request = updateFrom(salon);
        request.setOpeningTime(LocalTime.of(19, 0));
        request.setClosingTime(LocalTime.of(23, 0));

        assertTrue(service.updateSalon(request, 1L, 14L).isSuccess());

        request.setOpeningTime(LocalTime.of(22, 0));
        request.setClosingTime(LocalTime.of(21, 0));
        assertFalse(service.updateSalon(request, 1L, 14L).isSuccess());
    }

    private Salon ownedSalon(long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Salon salon = new Salon();
        salon.setId(12L);
        salon.setOwner(owner);
        salon.setLocation("Amman");
        // Older data that fails today's full-profile rules must not block a pin update.
        salon.setDescription("short");
        when(salonRepo.findById(salon.getId())).thenReturn(Optional.of(salon));
        return salon;
    }

    @Test
    void ownerSavesOnlyTheMapPinAndTheServerBuildsTheLink() {
        Salon salon = ownedSalon(4L);

        ApiResponse<SalonResponse> response = service.updateSalonLocation(
                12L, 4L, new UpdateSalonLocationRequest(32.5556123456, 35.85, "Irbid"));

        assertTrue(response.isSuccess());
        assertEquals(32.5556123, salon.getLatitude());
        assertEquals(35.85, salon.getLongitude());
        assertEquals("Irbid", salon.getLocation());
        assertEquals("short", salon.getDescription());
        assertEquals("https://www.google.com/maps/search/?api=1&query=32.5556123,35.85",
                response.getData().googleMapsUrl());
        verify(salonRepo).save(salon);
    }

    @Test
    void unsupportedDetectedCityKeepsTheCurrentCity() {
        Salon salon = ownedSalon(4L);

        ApiResponse<SalonResponse> response = service.updateSalonLocation(
                12L, 4L, new UpdateSalonLocationRequest(31.95, 35.91, null));

        assertTrue(response.isSuccess());
        assertEquals("Amman", salon.getLocation());
    }

    @Test
    void ownerRemovesTheMapPin() {
        Salon salon = ownedSalon(4L);
        salon.setLatitude(31.95);
        salon.setLongitude(35.91);
        salon.setGoogleMapsUrl("https://www.google.com/maps/search/?api=1&query=31.95,35.91");

        ApiResponse<SalonResponse> response = service.updateSalonLocation(
                12L, 4L, new UpdateSalonLocationRequest(null, null, null));

        assertTrue(response.isSuccess());
        assertNull(salon.getLatitude());
        assertNull(salon.getLongitude());
        assertNull(salon.getGoogleMapsUrl());
    }

    @Test
    void anotherUserCannotMoveTheSalonPin() {
        ownedSalon(4L);

        ApiResponse<SalonResponse> response = service.updateSalonLocation(
                12L, 99L, new UpdateSalonLocationRequest(31.95, 35.91, null));

        assertFalse(response.isSuccess());
        assertEquals("You can only update your salon", response.getMessage());
        verify(salonRepo, never()).save(any());
    }

    @Test
    void pinOutsideJordanIsRejected() {
        ownedSalon(4L);

        ApiResponse<SalonResponse> response = service.updateSalonLocation(
                12L, 4L, new UpdateSalonLocationRequest(25.2048, 55.2708, null));

        assertFalse(response.isSuccess());
        assertEquals("The selected salon location must be inside Jordan", response.getMessage());
        verify(salonRepo, never()).save(any());
    }
}
