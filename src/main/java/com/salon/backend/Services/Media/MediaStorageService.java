package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {
    ApiResponse<MediaUploadResult> upload(MultipartFile file, String folder);
    ApiResponse<MediaUploadResult> uploadRemote(String url, String folder);
    ApiResponse<Void> delete(String publicId, String resourceType);
    boolean isConfigured();

    /**
     * Deletes a file of this Cloudinary account by its delivery URL. Used for
     * records that kept only the URL. Links to other sites are ignored.
     */
    default ApiResponse<Void> deleteByUrl(String url) {
        return ApiResponse.error("Deleting by URL is not supported.");
    }
}
