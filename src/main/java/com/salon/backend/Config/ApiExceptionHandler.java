package com.salon.backend.Config;

import com.salon.backend.DTOs.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Keeps framework-level failures (malformed JSON, wrong parameter types, unsupported methods,
 * oversized uploads, unexpected errors) inside the same {@link ApiResponse} envelope that
 * services already return. HTTP status codes stay exactly what Spring MVC would choose;
 * only the body changes from Spring's default error format to the API contract.
 *
 * Business outcomes are unaffected: services keep returning {@code ApiResponse.error(...)}.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String INTERNAL_ERROR_MESSAGE =
            "Something went wrong on our side. Please try again in a moment.";

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        if (headers != null) {
            // Preserve framework headers such as Allow (405) and Accept (415).
            responseHeaders.putAll(headers);
        }
        responseHeaders.remove(HttpHeaders.CONTENT_TYPE);
        responseHeaders.remove(HttpHeaders.CONTENT_LENGTH);
        return ResponseEntity.status(statusCode)
                .headers(responseHeaders)
                .body(ApiResponse.error(messageFor(statusCode)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) throws Exception {
        if (exception instanceof AuthenticationException || exception instanceof AccessDeniedException) {
            // Let Spring Security's entry point / access-denied handler produce the 401/403 envelope.
            throw exception;
        }
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(INTERNAL_ERROR_MESSAGE));
    }

    // Framework exception messages can expose internal class or parser details,
    // so clients receive stable, readable text derived from the status only.
    static String messageFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "The request is invalid. Please check the submitted information and try again.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "This operation is not supported for the requested resource.";
            case 406 -> "The requested response format is not supported.";
            case 413 -> "The uploaded file is too large.";
            case 415 -> "The request content type is not supported.";
            default -> statusCode.is5xxServerError()
                    ? INTERNAL_ERROR_MESSAGE
                    : "The request could not be completed.";
        };
    }
}
