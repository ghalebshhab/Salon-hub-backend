package com.salon.backend.Controllers.Auth;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Login.LoginRequest;
import com.salon.backend.DTOs.Auth.Login.LoginResponse;
import com.salon.backend.DTOs.Auth.Rigester.RigesterRequest;
import com.salon.backend.DTOs.Auth.Rigester.RigesterResponse;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.AuthService;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepo userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    @PostMapping({"/register", "/rigester"})
    public ApiResponse<RigesterResponse> rigester(@RequestBody RigesterRequest request) {

        return authService.Rigester(request);
    }
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {


        return authService.Login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                java.util.Date expiration = jwtService.extractExpiration(token);
                tokenBlacklistService.blacklistToken(token, expiration);
            } catch (Exception e) {
                // Token is invalid or already expired - still treat as logout
            }
        }
        return ResponseEntity.ok(ApiResponse.success( "Logged out successfully",null));
    }
}
