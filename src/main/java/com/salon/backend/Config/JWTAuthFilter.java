package com.salon.backend.Config;

import com.salon.backend.Services.Auth.Security.CustomUserDetailsService;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Session.AuthCookies;
import com.salon.backend.Services.Auth.Session.AuthSessionService;
import com.salon.backend.Services.Activity.UserActivityTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;
    private final AuthCookies authCookies;
    private final UserActivityTrackingService userActivityTrackingService;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // skip only public auth endpoints
        if (path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/rigester") ||
                path.equals("/api/auth/email/verify") ||
                path.equals("/api/auth/email/resend") ||
                path.equals("/api/auth/password/forgot") ||
                path.equals("/api/auth/password/verify-code") ||
                path.equals("/api/auth/password/reset") ||
                path.equals("/api/auth/refresh")) {

            filterChain.doFilter(request, response);
            return;
        }

        String jwt = resolveToken(request);
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.extractUserId(jwt);
            // Durable revocation: the token is valid only while its session row is active.
            if (!authSessionService.isActive(jwtService.extractSessionId(jwt), userId)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadById(userId);

                if (userDetails.isEnabled()
                        && jwtService.isTokenValid(
                        jwt,
                        userDetails.getId(),
                        userDetails.getCredentialsUpdatedAt())) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    try {
                        userActivityTrackingService.recordAuthenticatedActivity(userId);
                    } catch (RuntimeException activityError) {
                        log.debug("User activity tracking failed: {}", activityError.getMessage());
                    }

                }
            }

        } catch (Exception ex) {
            log.debug("JWT authentication failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * The web application sends the token in an httpOnly cookie. The admin dashboard's
     * server sends it as a Bearer header; the header wins when both are present.
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authCookies.readAccessToken(request).orElse(null);
    }
}
