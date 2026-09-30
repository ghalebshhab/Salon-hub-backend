package com.salon.backend.Services.User.Profile;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.Profile.CareerProfileRequest;
import com.salon.backend.DTOs.User.Profile.UserProfileResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Utils.UrlValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepo userRepo;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getProfile(Long userId) {
        User user = userRepo.findById(userId).orElse(null);

        if (user == null) {
            return ApiResponse.error("User profile not found");
        }

        return ApiResponse.success("User profile fetched successfully", mapToResponse(user));
    }

    @Override
    @Transactional
    public ApiResponse<UserProfileResponse> enableOrUpdateCareerProfile(
            CareerProfileRequest request,
            Long userId) {

        User user = userRepo.findById(userId).orElse(null);

        if (user == null) {
            return ApiResponse.error("User profile not found");
        }

        ApiResponse<Void> validationResponse = validateCareerProfile(request);
        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        user.setDateOfBirth(request.getDateOfBirth());
        user.setYearsOfExperience(request.getYearsOfExperience());
        user.setCareerSkills(normalizeValues(request.getSkills()));
        user.setPreferredWorkLocations(normalizeValues(request.getPreferredWorkLocations()));
        user.setResumeUrl(normalizeOptionalText(request.getResumeUrl()));
        user.setPortfolioUrl(normalizeOptionalText(request.getPortfolioUrl()));
        user.setProfessionalBio(normalizeOptionalText(request.getProfessionalBio()));
        user.setCareerProfileEnabled(true);

        userRepo.save(user);

        return ApiResponse.success(
                "Career mode enabled and profile updated successfully",
                mapToResponse(user)
        );
    }

    private ApiResponse<Void> validateCareerProfile(CareerProfileRequest request) {
        if (request == null) {
            return ApiResponse.error("Career profile information is required");
        }

        if (request.getDateOfBirth() == null
                || !request.getDateOfBirth().isBefore(LocalDate.now())) {
            return ApiResponse.error("A valid date of birth is required");
        }

        if (Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            return ApiResponse.error("Career profile users must be at least 18 years old");
        }

        if (request.getYearsOfExperience() == null
                || request.getYearsOfExperience() < 0) {
            return ApiResponse.error("Years of experience is required and cannot be negative");
        }

        if (normalizeValues(request.getSkills()).isEmpty()) {
            return ApiResponse.error("At least one career skill is required");
        }

        if (normalizeValues(request.getPreferredWorkLocations()).isEmpty()) {
            return ApiResponse.error("At least one preferred work location is required");
        }

        if (request.getProfessionalBio() != null
                && request.getProfessionalBio().trim().length() > 2000) {
            return ApiResponse.error("Professional biography cannot exceed 2000 characters");
        }

        String resumeUrl = normalizeOptionalText(request.getResumeUrl());
        if (resumeUrl != null && !UrlValidationUtils.isValidPdfUrl(resumeUrl)) {
            return ApiResponse.error(
                    "Resume URL must be a valid HTTP or HTTPS link to a PDF file"
            );
        }

        String portfolioUrl = normalizeOptionalText(request.getPortfolioUrl());
        if (portfolioUrl != null && !UrlValidationUtils.isValidWebUrl(portfolioUrl)) {
            return ApiResponse.error(
                    "Portfolio URL must be a valid HTTP or HTTPS link"
            );
        }

        return ApiResponse.success("Career profile information is valid", null);
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null) {
            return List.of();
        }

        Map<String, String> normalizedValues = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            String trimmedValue = value.trim();
            normalizedValues.putIfAbsent(
                    trimmedValue.toLowerCase(Locale.ROOT),
                    trimmedValue
            );
        }

        return new ArrayList<>(normalizedValues.values());
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserProfileResponse mapToResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getSalon() == null ? null : user.getSalon().getId(),
                user.getSalon() == null ? null : user.getSalon().getName(),
                user.isCareerProfileEnabled(),
                user.getDateOfBirth(),
                user.getYearsOfExperience(),
                safeList(user.getCareerSkills()),
                safeList(user.getPreferredWorkLocations()),
                user.getResumeUrl(),
                user.getResumeUrl() != null,
                user.getPortfolioUrl(),
                user.getProfessionalBio(),
                calculateCompletionPercentage(user)
        );
    }

    private int calculateCompletionPercentage(User user) {
        int percentage = 0;
        percentage += user.getDateOfBirth() == null ? 0 : 20;
        percentage += user.getYearsOfExperience() == null ? 0 : 20;
        percentage += safeList(user.getCareerSkills()).isEmpty() ? 0 : 20;
        percentage += safeList(user.getPreferredWorkLocations()).isEmpty() ? 0 : 20;
        percentage += user.getResumeUrl() == null ? 0 : 10;
        percentage += user.getPortfolioUrl() == null ? 0 : 5;
        percentage += user.getProfessionalBio() == null ? 0 : 5;
        return percentage;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
