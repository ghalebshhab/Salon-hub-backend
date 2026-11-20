package com.salon.backend.Config;

import com.salon.backend.Services.Auth.Security.CustomUserDetailsService;
import com.salon.backend.Services.Auth.Security.ApiAuthenticationEntryPoint;
import com.salon.backend.Services.Auth.Security.ApiAccessDeniedHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@RequiredArgsConstructor
@Configuration
// This annotation tell the spring to open this file when the server is start running , becuase it contains a important things .
public class SecurityConfig {

    private final JWTAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174,http://localhost:4173,http://127.0.0.1:4173,http://localhost:3000,http://127.0.0.1:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                //this csrf is used to prevent the cokkies , to prevent the browser to remember the users .
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Error dispatches only render the original failure; without this, any
                        // non-business error on a public route was reported to anonymous callers
                        // as 401 "session expired" instead of its real status.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/rigester",
                                "/api/auth/email/verify",
                                "/api/auth/email/resend",
                                "/api/auth/password/forgot",
                                "/api/auth/password/verify-code",
                                "/api/auth/password/reset",
                                "/api/dashboard/login")
                        .permitAll()
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/owner/dashboard").hasRole("OWNER")
                        .requestMatchers("/api/owner/payments/**").hasRole("OWNER")
                        .requestMatchers("/api/owner/subscription/**", "/api/owner/subscription").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/subscription-plans").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/my-salon",
                                "/api/bookings/my-salon/pending",
                                "/api/salon-employees/my-salon",
                                "/api/salon-services/my-salon")
                        .hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/salons").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/salons/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/salons/active", "/api/salons/{salonId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/closures").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salons/*/payment-options").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salon-services/salon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salon-employees/salon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/salons/*/work").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/salon/*",
                                "/api/reviews/salon/*/summary")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/assistant/chat").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/assistant/insights").permitAll()
                        .anyRequest().authenticated())
                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
