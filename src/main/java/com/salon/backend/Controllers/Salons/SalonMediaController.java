package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaPageResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaResponse;
import com.salon.backend.DTOs.Media.MediaUploadResponse;
import com.salon.backend.DTOs.Media.RemoteMediaUploadRequest;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Media.SalonMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class SalonMediaController {

    private final SalonMediaService salonMediaService;

    @PostMapping(value = "/salon-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaUploadResponse> uploadSalonCover(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.uploadSalonCover(currentUser.getId(), file);
    }

    @PostMapping("/salon-cover/drive")
    public ApiResponse<MediaUploadResponse> uploadSalonCoverFromDrive(
            @RequestBody RemoteMediaUploadRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.uploadSalonCoverFromDrive(currentUser.getId(), request.url());
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaUploadResponse> uploadProfilePicture(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.uploadProfilePicture(currentUser.getId(), file);
    }

    @PostMapping(value = "/work", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmployeeWorkMediaResponse> addWorkMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.addWorkMedia(currentUser.getId(), file, caption);
    }

    @PostMapping("/work/drive")
    public ApiResponse<EmployeeWorkMediaResponse> addWorkMediaFromDrive(
            @RequestBody RemoteMediaUploadRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.addWorkMediaFromDrive(currentUser.getId(), request.url(), request.caption());
    }

    @GetMapping("/salons/{salonId}/work")
    public ApiResponse<EmployeeWorkMediaPageResponse> getSalonWorkMedia(
            @PathVariable Long salonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return salonMediaService.getSalonWorkMedia(salonId, page, size);
    }

    @DeleteMapping("/work/{mediaId}")
    public ApiResponse<Void> deleteWorkMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return salonMediaService.deleteWorkMedia(currentUser.getId(), mediaId);
    }
}
