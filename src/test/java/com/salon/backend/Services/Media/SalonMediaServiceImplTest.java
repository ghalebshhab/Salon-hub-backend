package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaResponse;
import com.salon.backend.DTOs.Media.MediaUploadResponse;
import com.salon.backend.Entities.media.EmployeeWorkMedia;
import com.salon.backend.Entities.media.MediaResourceType;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Media.EmployeeWorkMediaRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonMediaServiceImplTest {

    @Mock private MediaStorageService storageService;
    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @Mock private EmployeeWorkMediaRepo workMediaRepo;
    @InjectMocks private SalonMediaServiceImpl service;

    @Test
    void publicGalleryUsesCurrentActiveEmployeesAndClampsPagination() {
        Salon salon = salon(3L, user(7L, UserRole.OWNER));
        when(salonRepo.findById(3L)).thenReturn(Optional.of(salon));
        when(workMediaRepo.findPublicSalonWork(3L, UserRole.Employee, UserStatus.Active,
                PageRequest.of(0, 24))).thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 24), 0));

        var response = service.getSalonWorkMedia(3L, -1, 100);

        assertTrue(response.isSuccess());
        verify(workMediaRepo).findPublicSalonWork(3L, UserRole.Employee, UserStatus.Active,
                PageRequest.of(0, 24));
    }

    @Test
    void blockedSalonCannotExposePublicGallery() {
        Salon salon = salon(3L, user(7L, UserRole.OWNER));
        salon.setBlockedByOwner(true);
        when(salonRepo.findById(3L)).thenReturn(Optional.of(salon));

        var response = service.getSalonWorkMedia(3L, 0, 24);

        assertFalse(response.isSuccess());
        verifyNoInteractions(workMediaRepo);
    }

    @Test
    void ownerCoverReplacementStoresCloudinaryMetadataAndDeletesThePreviousAsset() {
        User owner = user(7L, UserRole.OWNER);
        Salon salon = salon(3L, owner);
        salon.setProfilePicturePublicId("salonhub/old-cover");
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});
        MediaUploadResult uploaded = new MediaUploadResult(
                "https://res.cloudinary.com/demo/image/upload/new.jpg",
                "salonhub/new-cover",
                MediaResourceType.IMAGE,
                1600,
                900,
                5000L
        );
        when(salonRepo.findByOwnerId(owner.getId())).thenReturn(salon);
        when(storageService.upload(file, "salonhub/salon-covers"))
                .thenReturn(ApiResponse.success("Uploaded", uploaded));
        when(storageService.delete("salonhub/old-cover", "image"))
                .thenReturn(ApiResponse.success("Deleted", null));

        ApiResponse<MediaUploadResponse> response = service.uploadSalonCover(owner.getId(), file);

        assertTrue(response.isSuccess());
        assertEquals(uploaded.secureUrl(), salon.getProfilePictureUrl());
        assertEquals(uploaded.publicId(), salon.getProfilePicturePublicId());
        verify(salonRepo).save(salon);
        verify(storageService).delete("salonhub/old-cover", "image");
    }

    @Test
    void activeEmployeeCanPublishWorkOnlyToTheirCurrentSalon() {
        User employee = user(11L, UserRole.Employee);
        employee.setCareerSkills(java.util.List.of("Colorist"));
        Salon salon = salon(3L, user(7L, UserRole.OWNER));
        employee.setSalon(salon);
        MockMultipartFile file = new MockMultipartFile("file", "result.webp", "image/webp", new byte[]{1});
        MediaUploadResult uploaded = new MediaUploadResult(
                "https://res.cloudinary.com/demo/image/upload/work.webp",
                "salonhub/work-1",
                MediaResourceType.IMAGE,
                900,
                900,
                4500L
        );
        when(userRepo.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(storageService.upload(file, "salonhub/employee-work"))
                .thenReturn(ApiResponse.success("Uploaded", uploaded));

        ApiResponse<EmployeeWorkMediaResponse> response =
                service.addWorkMedia(employee.getId(), file, "  Soft balayage  ");

        assertTrue(response.isSuccess());
        ArgumentCaptor<EmployeeWorkMedia> captor = ArgumentCaptor.forClass(EmployeeWorkMedia.class);
        verify(workMediaRepo).save(captor.capture());
        EmployeeWorkMedia saved = captor.getValue();
        assertEquals(salon, saved.getSalon());
        assertEquals(employee, saved.getEmployee());
        assertEquals("Soft balayage", saved.getCaption());
        assertEquals(uploaded.publicId(), saved.getPublicId());
    }

    @Test
    void failedProviderDeletionPreservesTheDatabaseRecordForRetry() {
        EmployeeWorkMedia media = new EmployeeWorkMedia();
        media.setPublicId("salonhub/work");
        media.setResourceType(MediaResourceType.IMAGE);
        when(workMediaRepo.findByIdAndEmployeeId(55L, 11L)).thenReturn(Optional.of(media));
        when(storageService.delete("salonhub/work", "image"))
                .thenReturn(ApiResponse.error("Media deletion is not configured."));

        var response = service.deleteWorkMedia(11L, 55L);

        assertFalse(response.isSuccess());
        assertEquals("Media deletion is not configured.", response.getMessage());
        verify(workMediaRepo, never()).delete(any());
        assertEquals("salonhub/work", media.getPublicId());
    }

    @Test
    void employeeCannotDeleteAnotherEmployeesMedia() {
        when(workMediaRepo.findByIdAndEmployeeId(55L, 11L)).thenReturn(Optional.empty());

        ApiResponse<Void> response = service.deleteWorkMedia(11L, 55L);

        assertFalse(response.isSuccess());
        verifyNoInteractions(storageService);
        verify(workMediaRepo, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oversizedCaptionIsRejectedBeforeAnyProviderOperation(boolean drive) {
        User employee = user(11L, UserRole.Employee);
        employee.setSalon(salon(3L, user(7L, UserRole.OWNER)));
        when(userRepo.findById(11L)).thenReturn(Optional.of(employee));

        var response = publish(drive, "  " + "x".repeat(301) + "  ");

        assertFalse(response.isSuccess());
        assertEquals("A work-media caption cannot exceed 300 characters.", response.getMessage());
        assertNull(response.getData());
        verifyNoInteractions(storageService, workMediaRepo);
    }

    @ParameterizedTest
    @MethodSource("acceptedCaptions")
    void acceptedCaptionIsNormalizedAndSavedForBothUploadSources(String caption) {
        User employee = user(11L, UserRole.Employee);
        employee.setSalon(salon(3L, user(7L, UserRole.OWNER)));
        when(userRepo.findById(11L)).thenReturn(Optional.of(employee));
        MediaUploadResult uploaded = new MediaUploadResult(
                "https://res.cloudinary.com/demo/image/upload/work.webp",
                "salonhub/work-1", MediaResourceType.IMAGE, 900, 900, 4500L);
        when(storageService.upload(any(), eq("salonhub/employee-work")))
                .thenReturn(ApiResponse.success("Uploaded", uploaded));
        when(storageService.uploadRemote(anyString(), eq("salonhub/employee-work")))
                .thenReturn(ApiResponse.success("Uploaded", uploaded));

        assertTrue(publish(false, caption).isSuccess());
        assertTrue(publish(true, caption).isSuccess());

        ArgumentCaptor<EmployeeWorkMedia> captor = ArgumentCaptor.forClass(EmployeeWorkMedia.class);
        verify(workMediaRepo, times(2)).save(captor.capture());
        String expected = caption == null || caption.trim().isBlank() ? null : caption.trim();
        for (EmployeeWorkMedia saved : captor.getAllValues()) {
            assertEquals(expected, saved.getCaption());
            assertEquals(employee, saved.getEmployee());
            assertEquals(employee.getSalon(), saved.getSalon());
        }
        verify(storageService, never()).delete(anyString(), anyString());
    }

    static Stream<String> acceptedCaptions() {
        return Stream.of(null, "   ", "  Soft balayage  ", "  " + "x".repeat(300) + "  ");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unauthorizedPublisherIsRejectedBeforeCaptionValidationOrUpload(boolean drive) {
        when(userRepo.findById(11L)).thenReturn(Optional.empty());

        var response = publish(drive, "x".repeat(301));

        assertFalse(response.isSuccess());
        assertEquals("Only an active salon employee can publish work media.", response.getMessage());
        verifyNoInteractions(storageService, workMediaRepo);
    }

    private ApiResponse<EmployeeWorkMediaResponse> publish(boolean drive, String caption) {
        return drive
                ? service.addWorkMediaFromDrive(11L, "https://drive.google.com/file/d/work/view", caption)
                : service.addWorkMedia(11L,
                        new MockMultipartFile("file", "work.webp", "image/webp", new byte[]{1}), caption);
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(UserStatus.Active);
        user.setFirstName(role == UserRole.Employee ? "Lina" : "Owner");
        return user;
    }

    private Salon salon(Long id, User owner) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        return salon;
    }
}
