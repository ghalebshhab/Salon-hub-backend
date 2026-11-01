package com.salon.backend.DTOs.Media;

import com.salon.backend.Entities.media.MediaResourceType;

public record MediaUploadResponse(
        String secureUrl,
        String publicId,
        MediaResourceType resourceType,
        Integer width,
        Integer height,
        Long bytes
) {}
