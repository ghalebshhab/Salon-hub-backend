package com.salon.backend.Services.Media;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.media.MediaResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CloudinaryMediaStorageService implements MediaStorageService {

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 40L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");
    private static final Set<String> REMOTE_HOSTS = Set.of(
            "drive.google.com", "docs.google.com", "lh3.googleusercontent.com",
            "googleusercontent.com");
    private static final Pattern DRIVE_FILE_PATH = Pattern.compile("/file/d/([A-Za-z0-9_-]+)");

    private final RestClient restClient;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    @Autowired
    public CloudinaryMediaStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this(cloudName, apiKey, apiSecret, RestClient.builder());
    }

    CloudinaryMediaStorageService(String cloudName, String apiKey, String apiSecret,
                                 RestClient.Builder clientBuilder) {
        this.restClient = clientBuilder.build();
        this.cloudName = cloudName == null ? "" : cloudName.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
    }

    @Override
    public ApiResponse<MediaUploadResult> upload(MultipartFile file, String folder) {
        if (!isConfigured()) {
            return ApiResponse.error("Media uploads are not configured. Add the Cloudinary environment credentials and restart the backend.");
        }
        String validationError = validateFile(file);
        if (validationError != null) {
            return ApiResponse.error(validationError);
        }
        try {
            MultiValueMap<String, Object> body = signedBody(folder);
            body.add("file", file.getResource());
            return executeUpload(body);
        } catch (Exception exception) {
            log.warn("Cloudinary upload failed: {}", describe(exception));
            return ApiResponse.error("The media file could not be uploaded. Please verify the file and try again.");
        }
    }

    @Override
    public ApiResponse<MediaUploadResult> uploadRemote(String url, String folder) {
        if (!isConfigured()) {
            return ApiResponse.error("Media uploads are not configured. Add the Cloudinary environment credentials and restart the backend.");
        }
        String normalized = normalizeRemoteUrl(url);
        if (normalized == null) {
            return ApiResponse.error("Enter a valid public HTTPS Google Drive image or video link.");
        }
        try {
            MultiValueMap<String, Object> body = signedBody(folder);
            body.add("file", normalized);
            return executeUpload(body);
        } catch (Exception exception) {
            log.warn("Cloudinary remote upload failed: {}", describe(exception));
            return ApiResponse.error("Cloudinary could not read that Google Drive file. Make sure its sharing setting is 'Anyone with the link'.");
        }
    }

    @Override
    public ApiResponse<Void> delete(String publicId, String resourceType) {
        if (!isConfigured()) {
            return ApiResponse.error("Media deletion is not configured. Add the Cloudinary environment credentials and retry.");
        }
        if (publicId == null || publicId.isBlank()) {
            return ApiResponse.error("The stored media asset identifier is missing.");
        }
        if (!"image".equalsIgnoreCase(resourceType) && !"video".equalsIgnoreCase(resourceType)) {
            return ApiResponse.error("The stored media resource type is not supported.");
        }
        String normalizedType = resourceType.toLowerCase(Locale.ROOT);
        long timestamp = Instant.now().getEpochSecond();
        String parameters = "invalidate=true&public_id=" + publicId + "&timestamp=" + timestamp;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("public_id", publicId);
        body.add("timestamp", Long.toString(timestamp));
        body.add("invalidate", "true");
        body.add("api_key", apiKey);
        body.add("signature", sha1(parameters + apiSecret));
        try {
            Map<?, ?> response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloud}/{type}/destroy", cloudName, normalizedType)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !("ok".equals(response.get("result"))
                    || "not found".equals(response.get("result")))) {
                return ApiResponse.error("Cloudinary did not confirm deletion of the stored media. Please retry.");
            }
            return ApiResponse.success("Media deleted", null);
        } catch (Exception exception) {
            log.warn("Cloudinary delete failed for {}: {}", publicId, describe(exception));
            return ApiResponse.error("The stored media could not be deleted from Cloudinary.");
        }
    }

    @Override
    public boolean isConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private MultiValueMap<String, Object> signedBody(String folder) {
        String safeFolder = folder == null || folder.isBlank() ? "salonhub" : folder;
        long timestamp = Instant.now().getEpochSecond();
        String parameters = "folder=" + safeFolder + "&timestamp=" + timestamp;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("folder", safeFolder);
        body.add("timestamp", Long.toString(timestamp));
        body.add("api_key", apiKey);
        body.add("signature", sha1(parameters + apiSecret));
        return body;
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<MediaUploadResult> executeUpload(MultiValueMap<String, Object> body) {
        Map<String, Object> response = restClient.post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/auto/upload", cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("secure_url") == null || response.get("public_id") == null) {
            return ApiResponse.error("Cloudinary did not return a usable media URL.");
        }
        String type = String.valueOf(response.getOrDefault("resource_type", "image"));
        MediaUploadResult result = new MediaUploadResult(
                String.valueOf(response.get("secure_url")),
                String.valueOf(response.get("public_id")),
                "video".equalsIgnoreCase(type) ? MediaResourceType.VIDEO : MediaResourceType.IMAGE,
                number(response.get("width")),
                number(response.get("height")),
                longNumber(response.get("bytes"))
        );
        return ApiResponse.success("Media uploaded successfully", result);
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Choose an image or video file to upload.";
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        boolean image = IMAGE_TYPES.contains(contentType);
        boolean video = VIDEO_TYPES.contains(contentType);
        if (!image && !video) {
            return "Only JPG, PNG, WEBP, GIF, MP4, and WEBM files are supported.";
        }
        long limit = image ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES;
        if (file.getSize() > limit) {
            return image ? "Images must be 8 MB or smaller." : "Videos must be 40 MB or smaller.";
        }
        try {
            byte[] header;
            try (var input = file.getInputStream()) {
                header = input.readNBytes(16);
            }
            if (!matchesSignature(header, contentType)) {
                return "The selected file contents do not match its declared media type.";
            }
        } catch (Exception exception) {
            return "The selected file could not be inspected.";
        }
        return null;
    }

    private boolean matchesSignature(byte[] bytes, String type) {
        if (bytes.length < 4) return false;
        if ("image/jpeg".equals(type)) return unsigned(bytes[0]) == 0xff && unsigned(bytes[1]) == 0xd8 && unsigned(bytes[2]) == 0xff;
        if ("image/png".equals(type)) return unsigned(bytes[0]) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        if ("image/gif".equals(type)) return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
        if ("image/webp".equals(type)) return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        if ("video/mp4".equals(type)) return bytes.length >= 8 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
        return "video/webm".equals(type) && unsigned(bytes[0]) == 0x1a && unsigned(bytes[1]) == 0x45 && unsigned(bytes[2]) == 0xdf && unsigned(bytes[3]) == 0xa3;
    }

    private String normalizeRemoteUrl(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null) return null;
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowed = REMOTE_HOSTS.stream().anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));
            if (!allowed) return null;
            if (host.equals("drive.google.com")) {
                Matcher matcher = DRIVE_FILE_PATH.matcher(uri.getPath() == null ? "" : uri.getPath());
                if (matcher.find()) {
                    return "https://drive.google.com/uc?export=download&id=" + matcher.group(1);
                }
            }
            return uri.toASCIIString();
        } catch (Exception exception) {
            return null;
        }
    }

    /** Cloudinary puts the real reason in the response body, e.g. a missing key permission. */
    private String describe(Exception exception) {
        if (exception instanceof RestClientResponseException response) {
            return response.getStatusCode() + " " + response.getResponseBodyAsString();
        }
        return exception.toString();
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }

    private int unsigned(byte value) { return value & 0xff; }
    private Integer number(Object value) { return value instanceof Number number ? number.intValue() : null; }
    private Long longNumber(Object value) { return value instanceof Number number ? number.longValue() : null; }
}
