package com.salon.backend.Controllers.Auth;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Login.LoginRequest;
import com.salon.backend.DTOs.Auth.Login.LoginResponse;
import com.salon.backend.DTOs.Auth.Rigester.RigesterRequest;
import com.salon.backend.DTOs.Auth.Rigester.RigesterResponse;
import com.salon.backend.DTOs.Auth.Verification.ChangePasswordRequest;
import com.salon.backend.DTOs.Auth.Verification.EmailAddressRequest;
import com.salon.backend.DTOs.Auth.Verification.PasswordResetAuthorizationResponse;
import com.salon.backend.DTOs.Auth.Verification.ResetPasswordRequest;
import com.salon.backend.DTOs.Auth.Verification.VerifyCodeRequest;
import com.salon.backend.Services.Auth.AuthService;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Auth.Security.TokenBlacklistService;
import com.salon.backend.Services.Auth.Verification.AccountVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final AccountVerificationService accountVerificationService;
    @PostMapping({"/register", "/rigester"})
    public ApiResponse<RigesterResponse> rigester(@RequestBody RigesterRequest request) {

        return authService.Rigester(request);
    }
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {


        return authService.Login(request);
    }

    @PostMapping("/email/verify")
    public ApiResponse<String> verifyEmail(@RequestBody VerifyCodeRequest request) {
        return accountVerificationService.verifyEmail(request);
    }

    @PostMapping("/email/resend")
    public ApiResponse<String> resendEmailVerification(@RequestBody EmailAddressRequest request) {
        return accountVerificationService.resendEmailVerification(request);
    }

    @PostMapping("/password/forgot")
    public ApiResponse<String> forgotPassword(@RequestBody EmailAddressRequest request) {
        return accountVerificationService.requestPasswordReset(request);
    }

    @PostMapping("/password/verify-code")
    public ApiResponse<PasswordResetAuthorizationResponse> verifyPasswordResetCode(
            @RequestBody VerifyCodeRequest request
    ) {
        return accountVerificationService.verifyPasswordResetCode(request);
    }

    @PostMapping("/password/reset")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return accountVerificationService.resetPassword(request);
    }

    @PostMapping("/password/change")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ChangePasswordRequest request
    ) {
        return accountVerificationService.changePassword(
                user == null ? null : user.getId(),
                request
        );
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
