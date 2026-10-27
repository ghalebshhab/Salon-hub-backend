package com.salon.backend.Services.Auth.Verification;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Verification.EmailAddressRequest;
import com.salon.backend.DTOs.Auth.Verification.PasswordResetAuthorizationResponse;
import com.salon.backend.DTOs.Auth.Verification.ResetPasswordRequest;
import com.salon.backend.DTOs.Auth.Verification.VerifyCodeRequest;
import com.salon.backend.Entities.users.User;

public interface AccountVerificationService {

    boolean issueRegistrationVerification(User user);

    ApiResponse<String> resendEmailVerification(EmailAddressRequest request);

    ApiResponse<String> verifyEmail(VerifyCodeRequest request);

    ApiResponse<String> requestPasswordReset(EmailAddressRequest request);

    ApiResponse<PasswordResetAuthorizationResponse> verifyPasswordResetCode(
            VerifyCodeRequest request
    );

    ApiResponse<String> resetPassword(ResetPasswordRequest request);
}
