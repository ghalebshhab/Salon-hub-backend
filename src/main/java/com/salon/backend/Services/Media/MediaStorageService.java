package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {
    ApiResponse<MediaUploadResult> upload(MultipartFile file, String folder);
    ApiResponse<MediaUploadResult> uploadRemote(String url, String folder);
    ApiResponse<Void> delete(String publicId, String resourceType);
    boolean isConfigured();
}
