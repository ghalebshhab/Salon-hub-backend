package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaPageResponse;
import com.salon.backend.DTOs.Media.EmployeeWorkMediaResponse;
import com.salon.backend.DTOs.Media.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SalonMediaService {
    ApiResponse<MediaUploadResponse> uploadSalonCover(Long ownerId, MultipartFile file);
    ApiResponse<MediaUploadResponse> uploadSalonCoverFromDrive(Long ownerId, String url);
    ApiResponse<EmployeeWorkMediaResponse> addWorkMedia(Long employeeId, MultipartFile file, String caption);
    ApiResponse<EmployeeWorkMediaResponse> addWorkMediaFromDrive(Long employeeId, String url, String caption);
    ApiResponse<EmployeeWorkMediaPageResponse> getSalonWorkMedia(Long salonId, int page, int size);
    ApiResponse<Void> deleteWorkMedia(Long employeeId, Long mediaId);
}
