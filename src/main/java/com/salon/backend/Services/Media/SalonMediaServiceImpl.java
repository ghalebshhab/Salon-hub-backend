package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaPageResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SalonMediaServiceImpl implements SalonMediaService {

    private final MediaStorageService storageService;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final EmployeeWorkMediaRepo workMediaRepo;

    @Override
    @Transactional
    public ApiResponse<MediaUploadResponse> uploadSalonCover(Long ownerId, MultipartFile file) {
        Salon salon = ownerSalon(ownerId);
        if (salon == null) return ApiResponse.error("Create your salon before uploading its profile picture.");
        ApiResponse<MediaUploadResult> uploaded = storageService.upload(file, "salonhub/salon-covers");
        return saveSalonCover(salon, uploaded);
    }

    @Override
    @Transactional
    public ApiResponse<MediaUploadResponse> uploadSalonCoverFromDrive(Long ownerId, String url) {
        Salon salon = ownerSalon(ownerId);
        if (salon == null) return ApiResponse.error("Create your salon before uploading its profile picture.");
        ApiResponse<MediaUploadResult> uploaded = storageService.uploadRemote(url, "salonhub/salon-covers");
        return saveSalonCover(salon, uploaded);
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeWorkMediaResponse> addWorkMedia(
            Long employeeId, MultipartFile file, String caption) {
        User employee = activeEmployee(employeeId);
        if (employee == null) return ApiResponse.error("Only an active salon employee can publish work media.");
        String normalizedCaption = normalizeCaption(caption);
        if (normalizedCaption != null && normalizedCaption.length() > 300) {
            return ApiResponse.error("A work-media caption cannot exceed 300 characters.");
        }
        ApiResponse<MediaUploadResult> uploaded = storageService.upload(file, "salonhub/employee-work");
        return saveWorkMedia(employee, uploaded, normalizedCaption);
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeWorkMediaResponse> addWorkMediaFromDrive(
            Long employeeId, String url, String caption) {
        User employee = activeEmployee(employeeId);
        if (employee == null) return ApiResponse.error("Only an active salon employee can publish work media.");
        String normalizedCaption = normalizeCaption(caption);
        if (normalizedCaption != null && normalizedCaption.length() > 300) {
            return ApiResponse.error("A work-media caption cannot exceed 300 characters.");
        }
        ApiResponse<MediaUploadResult> uploaded = storageService.uploadRemote(url, "salonhub/employee-work");
        return saveWorkMedia(employee, uploaded, normalizedCaption);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmployeeWorkMediaPageResponse> getSalonWorkMedia(Long salonId, int page, int size) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        if (salon == null || salon.getStatus() != SalonStatus.Accepted
                || salon.isBlockedByAdmin() || salon.isBlockedByOwner()) {
            return ApiResponse.error("Active salon not found");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(24, Math.max(1, size));
        var mediaPage = workMediaRepo.findPublicSalonWork(
                salonId, UserRole.Employee, UserStatus.Active, PageRequest.of(safePage, safeSize));
        return ApiResponse.success("Salon work media fetched successfully", new EmployeeWorkMediaPageResponse(
                mediaPage.getContent().stream().map(this::mapWorkMedia).toList(),
                mediaPage.getNumber(),
                mediaPage.getSize(),
                mediaPage.getTotalElements(),
                mediaPage.getTotalPages()
        ));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteWorkMedia(Long employeeId, Long mediaId) {
        User employee = activeEmployee(employeeId);
        if (employee == null) return ApiResponse.error("Only an active salon employee can manage work media.");
        EmployeeWorkMedia media = mediaId == null
                ? null
                : workMediaRepo.findByIdAndEmployeeId(mediaId, employeeId).orElse(null);
        if (media == null || media.getSalon() == null
                || !employee.getSalon().getId().equals(media.getSalon().getId())) {
            return ApiResponse.error("Work media was not found or does not belong to you in your current salon.");
        }
        ApiResponse<Void> deletion = storageService.delete(
                media.getPublicId(), media.getResourceType().name().toLowerCase());
        if (!deletion.isSuccess()) return ApiResponse.error(deletion.getMessage());
        workMediaRepo.delete(media);
        return ApiResponse.success("Work media deleted successfully", null);
    }

    private ApiResponse<MediaUploadResponse> saveSalonCover(
            Salon salon, ApiResponse<MediaUploadResult> uploaded) {
        if (!uploaded.isSuccess()) return ApiResponse.error(uploaded.getMessage());
        MediaUploadResult result = uploaded.getData();
        if (result.resourceType() != MediaResourceType.IMAGE) {
            storageService.delete(result.publicId(), "video");
            return ApiResponse.error("The salon profile picture must be an image.");
        }
        String previousPublicId = salon.getProfilePicturePublicId();
        salon.setProfilePictureUrl(result.secureUrl());
        salon.setProfilePicturePublicId(result.publicId());
        salonRepo.save(salon);
        if (previousPublicId != null && !previousPublicId.equals(result.publicId())) {
            storageService.delete(previousPublicId, "image");
        }
        return ApiResponse.success("Salon profile picture updated", mapUpload(result));
    }

    private String normalizeCaption(String caption) {
        if (caption == null) return null;
        String trimmed = caption.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private ApiResponse<EmployeeWorkMediaResponse> saveWorkMedia(
            User employee, ApiResponse<MediaUploadResult> uploaded, String normalizedCaption) {
        if (!uploaded.isSuccess()) return ApiResponse.error(uploaded.getMessage());
        MediaUploadResult result = uploaded.getData();
        EmployeeWorkMedia media = new EmployeeWorkMedia();
        media.setSalon(employee.getSalon());
        media.setEmployee(employee);
        media.setSecureUrl(result.secureUrl());
        media.setPublicId(result.publicId());
        media.setResourceType(result.resourceType());
        media.setCaption(normalizedCaption);
        media.setWidth(result.width());
        media.setHeight(result.height());
        media.setBytes(result.bytes());
        media.setCreatedAt(LocalDateTime.now());
        workMediaRepo.save(media);
        return ApiResponse.success("Work media published successfully", mapWorkMedia(media));
    }

    private Salon ownerSalon(Long ownerId) {
        if (ownerId == null) return null;
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return salon != null && salon.getStatus() != SalonStatus.DELETED ? salon : null;
    }

    private User activeEmployee(Long employeeId) {
        User user = employeeId == null ? null : userRepo.findById(employeeId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.Active
                || user.getRole() != UserRole.Employee || user.getSalon() == null) return null;
        Salon salon = user.getSalon();
        return salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin() && !salon.isBlockedByOwner() ? user : null;
    }

    private MediaUploadResponse mapUpload(MediaUploadResult result) {
        return new MediaUploadResponse(result.secureUrl(), result.publicId(), result.resourceType(),
                result.width(), result.height(), result.bytes());
    }

    private EmployeeWorkMediaResponse mapWorkMedia(EmployeeWorkMedia media) {
        User employee = media.getEmployee();
        String name = ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        if (name.isBlank()) name = employee.getUserName();
        String specialty = employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                ? null : employee.getCareerSkills().get(0);
        return new EmployeeWorkMediaResponse(media.getId(), employee.getId(), name, specialty, media.getSecureUrl(),
                media.getResourceType(), media.getCaption(), media.getWidth(), media.getHeight(),
                media.getCreatedAt());
    }
}
