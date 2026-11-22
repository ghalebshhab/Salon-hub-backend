package com.salon.backend.Config;

import tools.jackson.databind.ObjectMapper;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Services.Auth.Session.AuthCookies;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Second CSRF guard next to {@code SameSite=Strict}: a write request that relies on the
 * sign-in cookies must carry {@code X-Requested-With: XMLHttpRequest}. A plain HTML form
 * on another site cannot set this header, and a script on another site needs a CORS
 * pre-flight, which the CORS allow-list refuses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class CsrfHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Requested-With";
    public static final String EXPECTED = "XMLHttpRequest";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AuthCookies authCookies;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod())
                || !request.getServletPath().startsWith(AuthCookies.API_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (authCookies.hasSessionCookie(request)
                && !EXPECTED.equalsIgnoreCase(request.getHeader(HEADER))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(
                    "This request was blocked for your security. Reload the page and try again."));
            return;
        }
        chain.doFilter(request, response);
    }
}
