package com.salon.backend.Services.Media;

import com.salon.backend.Entities.media.MediaResourceType;

public record MediaUploadResult(
        String secureUrl,
        String publicId,
        MediaResourceType resourceType,
        Integer width,
        Integer height,
        Long bytes,
        Double durationSeconds
) {}
