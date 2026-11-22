package com.salon.backend.Services.Auth;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Login.LoginRequest;
import com.salon.backend.DTOs.Auth.Login.LoginResponse;
import com.salon.backend.DTOs.Auth.Rigester.RigesterRequest;
import com.salon.backend.DTOs.Auth.Rigester.RigesterResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Session.AuthSessionService;
import com.salon.backend.Services.Auth.Session.IssuedSession;
import com.salon.backend.Entities.auth.SessionClient;
import com.salon.backend.DTOs.Auth.Login.SignedIn;
import com.salon.backend.Services.Auth.Verification.AccountVerificationService;
import com.salon.backend.Services.Activity.UserActivityTrackingService;
import com.salon.backend.Utils.PasswordPolicy;
import com.salon.backend.Services.Email.EmailService;
import com.salon.backend.Services.Email.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthSessionService authSessionService;
    private final UserRepo userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AccountVerificationService accountVerificationService;
    private final UserActivityTrackingService userActivityTrackingService;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;

    /** Send a "new sign-in" email after each login. */
    @Value("${app.mail.login-alerts:true}")
    private boolean loginAlerts;

    private static final List<String> ALLOWED_EMAIL_DOMAINS = List.of(
            "gmail.com",
            "hotmail.com",
            "outlook.com",
            "yahoo.com",
            "icloud.com"
    );


    // =========================================================
    // REGISTER
    // =========================================================

    @Override
    @Transactional
    public ApiResponse<RigesterResponse> Rigester(
            RigesterRequest request) {

        // -----------------------------------------------------
        // Validate first name and last name
        // -----------------------------------------------------

        if (request.getFirstName() == null
                || request.getFirstName().isBlank()
                || request.getLastName() == null
                || request.getLastName().isBlank()) {

            return ApiResponse.error(
                    "First name and last name are required."
            );
        }

        if (request.getFirstName().trim().length() < 3
                || request.getLastName().trim().length() < 3) {

            return ApiResponse.error(
                    "First name and last name must contain at least 3 characters."
            );
        }


        // -----------------------------------------------------
        // Validate username
        // -----------------------------------------------------

        if (request.getUserName() == null
                || request.getUserName().isBlank()) {

            return ApiResponse.error(
                    "Username is required."
            );
        }

        String normalizedUsername =
                request.getUserName()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!normalizedUsername.matches(
                "^[a-z](?!(?:.*_){2})(?!(?:.*\\.){2})[a-z0-9_.]*$")) {

            return ApiResponse.error(
                    "Username must start with a lowercase letter and may contain only letters, numbers, underscores, and dots."
            );
        }


        // -----------------------------------------------------
        // Validate email
        // -----------------------------------------------------

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            return ApiResponse.error(
                    "Email address is required."
            );
        }

        String normalizedEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!isAllowedEmailDomain(normalizedEmail)) {

            return ApiResponse.error(
                    "Email address must use a valid email domain."
            );
        }


        // -----------------------------------------------------
        // Validate phone number
        // -----------------------------------------------------

        if (request.getPhoneNumber() == null
                || request.getPhoneNumber().isBlank()) {

            return ApiResponse.error(
                    "Phone number is required."
            );
        }

        String normalizedPhoneNumber =
                request.getPhoneNumber().trim();

        if (!normalizedPhoneNumber.matches("^\\+9627\\d{8}$")
                && !normalizedPhoneNumber.matches("^07\\d{8}$")) {

            return ApiResponse.error(
                    "Phone number must be in one of these formats: +9627XXXXXXXX or 07XXXXXXXX"
            );
        }


        // -----------------------------------------------------
        // Validate password
        // -----------------------------------------------------

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ApiResponse.error(
                    "Password is required."
            );
        }

        if (!PasswordPolicy.isValid(request.getPassword())) {

            return ApiResponse.error(
                    PasswordPolicy.REQUIREMENTS_MESSAGE
            );
        }


        // -----------------------------------------------------
        // Check email uniqueness
        // -----------------------------------------------------

        if (userRepo.existsByemail(normalizedEmail)) {

            return ApiResponse.error(
                    "Email address already exists."
            );
        }


        // -----------------------------------------------------
        // Check phone number uniqueness
        // -----------------------------------------------------

        if (userRepo.existsByphoneNumber(normalizedPhoneNumber)) {

            return ApiResponse.error(
                    "Phone number already exists."
            );
        }


        // -----------------------------------------------------
        // Check username uniqueness
        // -----------------------------------------------------

        if (userRepo.existsByuserName(normalizedUsername)) {

            return ApiResponse.error(
                    "Username already exists."
            );
        }


        // -----------------------------------------------------
        // Create user
        // -----------------------------------------------------

        User newUser = new User();

        newUser.setFirstName(request.getFirstName().trim());
        newUser.setLastName(request.getLastName().trim());

        newUser.setEmail(normalizedEmail);
        newUser.setUserName(normalizedUsername);

        newUser.setPassword(
                bCryptPasswordEncoder.encode(
                        request.getPassword()
                )
        );

        newUser.setPhoneNumber(normalizedPhoneNumber);
        newUser.setEmailVerified(false);

        newUser.setRole(UserRole.USER);
        newUser.setStatus(UserStatus.Active);
        initializeDefaultProfile(newUser);


        // -----------------------------------------------------
        // Save user
        // -----------------------------------------------------

        userRepo.save(newUser);
        boolean verificationEmailDelivered =
                accountVerificationService.issueRegistrationVerification(newUser);


        // -----------------------------------------------------
        // Create registration response
        // -----------------------------------------------------

        RigesterResponse response = new RigesterResponse();

        response.setFirstName(newUser.getFirstName());
        response.setLastName(newUser.getLastName());
        response.setEmail(newUser.getEmail());
        response.setUserName(newUser.getUserName());
        response.setId(newUser.getId());
        response.setPhoneNumber(newUser.getPhoneNumber());
        response.setEmailVerificationRequired(true);
        response.setVerificationCodeExpiresInSeconds(600);
        response.setResendAvailableInSeconds(60);


        String message = verificationEmailDelivered
                ? "Registered successfully. Check your email for the verification code."
                : "Registered successfully, but the verification email could not be delivered. Use resend verification to try again.";

        return ApiResponse.success(message, response);
    }

    private void initializeDefaultProfile(User user) {
        user.setCareerProfileEnabled(false);
        user.setCareerSkills(new ArrayList<>());
        user.setPreferredWorkLocations(new ArrayList<>());
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @Override
    public ApiResponse<SignedIn> Login(
            LoginRequest request, String userAgent) {

        // -----------------------------------------------------
        // Validate email
        // -----------------------------------------------------

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            return ApiResponse.error(
                    "Email address is required."
            );
        }

        String normalizedEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);


        // -----------------------------------------------------
        // Validate password
        // -----------------------------------------------------

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ApiResponse.error(
                    "Password is required."
            );
        }


        // -----------------------------------------------------
        // Find user
        // -----------------------------------------------------

        User user = userRepo.findByemail(normalizedEmail);

        if (user == null) {

            return ApiResponse.error(
                    "User with this email does not exist."
            );
        }


        // -----------------------------------------------------
        // Check account status
        // -----------------------------------------------------

        if (user.getStatus() != UserStatus.Active) {

            return ApiResponse.error(
                    "Your account is not active."
            );
        }


        // -----------------------------------------------------
        // Check password
        // -----------------------------------------------------

        if (!bCryptPasswordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            return ApiResponse.error(
                    "Incorrect password. Please try again."
            );
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            return ApiResponse.error(
                    "Email verification is required before signing in."
            );
        }


        // -----------------------------------------------------
        // Start a server-side session (the controller sets the cookies)
        // -----------------------------------------------------

        IssuedSession session =
                authSessionService.start(user, SessionClient.WEB, userAgent);

        try {
            userActivityTrackingService.recordSuccessfulLogin(user);
        } catch (RuntimeException ignored) {
            // Activity analytics must never prevent a valid user from signing in.
        }

        if (loginAlerts) {
            emailService.queue(user.getEmail(), user.getFirstName(),
                    emailTemplates.loginAlert(ZonedDateTime.now(ZoneId.of("Asia/Amman"))));
        }


        // -----------------------------------------------------
        // Create login response
        // -----------------------------------------------------

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUserName(),
                user.getRole(),
                user.getEmail()
        );


        return ApiResponse.success(
                "Logged in successfully. Loading...",
                new SignedIn(response, session)
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @Override
    public ApiResponse<ResponseEntity> Logout() {

        /*
         * JWT authentication is stateless.
         *
         * The actual logout implementation depends on
         * where the JWT is stored and whether we use
         * token revocation / refresh tokens.
         *
         * We should implement this together with the
         * authentication/security configuration.
         */

        return null;
    }


    // =========================================================
    // EMAIL DOMAIN VALIDATION
    // =========================================================

    private boolean isAllowedEmailDomain(String email) {

        int atIndex = email.lastIndexOf("@");

        if (atIndex == -1
                || atIndex == email.length() - 1) {

            return false;
        }

        String domain =
                email.substring(atIndex + 1)
                        .toLowerCase(Locale.ROOT);

        return ALLOWED_EMAIL_DOMAINS.contains(domain);
    }
}
