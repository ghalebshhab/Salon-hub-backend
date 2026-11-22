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
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Auth.Session.AuthCookies;
import com.salon.backend.Services.Auth.Session.AuthSessionService;
import com.salon.backend.DTOs.Auth.Login.SignedIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final AuthSessionService authSessionService;
    private final AuthCookies authCookies;
    private final AccountVerificationService accountVerificationService;
    @PostMapping({"/register", "/rigester"})
    public ApiResponse<RigesterResponse> rigester(@RequestBody RigesterRequest request) {

        return authService.Rigester(request);
    }
    /** Signs in and sets the httpOnly session cookies. The body holds no token. */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        ApiResponse<SignedIn> result = authService.Login(request, httpRequest.getHeader(HttpHeaders.USER_AGENT));
        if (!result.isSuccess() || result.getData() == null) {
            return ApiResponse.error(result.getMessage());
        }
        authCookies.write(httpResponse, result.getData().session());
        return ApiResponse.success(result.getMessage(), result.getData().account());
    }

    /** Rotates the refresh token and issues a new short-lived access token. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(HttpServletRequest httpRequest,
                                                       HttpServletResponse httpResponse) {
        return authCookies.readRefreshToken(httpRequest)
                .flatMap(authSessionService::refresh)
                .map(session -> {
                    authCookies.write(httpResponse, session);
                    return ResponseEntity.ok(ApiResponse.<String>success("Session refreshed", null));
                })
                .orElseGet(() -> {
                    authCookies.clear(httpResponse);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("Your session is missing or expired. Please sign in and try again."));
                });
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
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        String header = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = header != null && header.startsWith("Bearer ")
                ? header.substring(7)
                : authCookies.readAccessToken(httpRequest).orElse(null);
        authSessionService.signOut(accessToken, authCookies.readRefreshToken(httpRequest).orElse(null));
        authCookies.clear(httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
