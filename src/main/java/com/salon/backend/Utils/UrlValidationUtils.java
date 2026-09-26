package com.salon.backend.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public final class UrlValidationUtils {

    private static final Set<String> ALLOWED_WEB_SCHEMES = Set.of("http", "https");

    private UrlValidationUtils() {
    }

    public static boolean isValidWebUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();

            return scheme != null
                    && ALLOWED_WEB_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    public static boolean isValidPdfUrl(String value) {
        if (!isValidWebUrl(value)) {
            return false;
        }

        try {
            String path = new URI(value.trim()).getPath();
            return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
