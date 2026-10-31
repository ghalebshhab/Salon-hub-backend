package com.salon.backend.Config;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Services.Auth.Security.ApiAccessDeniedHandler;
import com.salon.backend.Services.Auth.Security.ApiAuthenticationEntryPoint;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Auth.Security.CustomUserDetailsService;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityResponseIntegrationTest.ProbeController.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, JWTAuthFilter.class, ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class, JwtService.class, TokenBlacklistService.class,
        SecurityResponseIntegrationTest.ProbeController.class})
class SecurityResponseIntegrationTest {

    private static final AtomicLong USER_IDS = new AtomicLong();

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    @Test
    void missingAuthenticationReturnsUnauthorizedEnvelope() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/salons/mine")));
    }

    @Test
    void anonymousAdminRequestReturnsUnauthorizedRatherThanForbidden() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/dashboard/users")));
    }

    @Test
    void malformedTokenReturnsUnauthorizedEnvelope() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/salons/mine")
                .header("Authorization", "Bearer invalid-token")));
    }

    @Test
    void blacklistedTokenReturnsUnauthorizedEnvelope() throws Exception {
        String token = tokenFor(UserRole.USER, UserStatus.Active);
        tokenBlacklistService.blacklistToken(token, jwtService.extractExpiration(token));

        assertUnauthorized(mockMvc.perform(get("/api/salons/mine")
                .header("Authorization", "Bearer " + token)));
    }

    @Test
    void blockedAccountReturnsUnauthorizedEnvelope() throws Exception {
        String token = tokenFor(UserRole.USER, UserStatus.Blocked);

        assertUnauthorized(mockMvc.perform(get("/api/salons/mine")
                .header("Authorization", "Bearer " + token)));
    }

    @Test
    void nonAdminReturnsForbiddenEnvelope() throws Exception {
        String token = tokenFor(UserRole.USER, UserStatus.Active);

        mockMvc.perform(get("/api/dashboard/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Your account does not have permission to perform this action."))
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void authenticatedUserCanReachProtectedEndpoint() throws Exception {
        String token = tokenFor(UserRole.USER, UserStatus.Active);

        mockMvc.perform(get("/api/salons/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminCanReachAdminEndpoint() throws Exception {
        String token = tokenFor(UserRole.ADMIN, UserStatus.Active);

        mockMvc.perform(get("/api/dashboard/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publicEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
        for (String path : new String[]{"/api/auth/login", "/api/salons/active"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    private void assertUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Your session is missing or expired. Please sign in and try again."))
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private String tokenFor(UserRole role, UserStatus status) {
        User user = new User();
        user.setId(USER_IDS.incrementAndGet());
        user.setEmail("security-test@example.com");
        user.setPassword("unused-test-password");
        user.setRole(role);
        user.setStatus(status);
        when(userDetailsService.loadById(user.getId())).thenReturn(AuthenticatedUser.from(user));
        return jwtService.generateToken(user);
    }

    // Probe responses isolate the production filter chain from controller business logic.
    @RestController
    static class ProbeController {
        @GetMapping({"/api/salons/mine", "/api/dashboard/users", "/api/auth/login", "/api/salons/active"})
        ApiResponse<Void> allowed() {
            return ApiResponse.success("Request allowed.", null);
        }
    }
}
