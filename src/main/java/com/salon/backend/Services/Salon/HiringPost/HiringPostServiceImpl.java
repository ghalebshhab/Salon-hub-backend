package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;
import com.salon.backend.DTOs.Salon.Employment.Join.AcceptRequest;
import com.salon.backend.DTOs.Salon.Employment.Join.RejectRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.EmploymentRequestSource;
import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.salons.employment.RequestType;
import com.salon.backend.Entities.salons.hiringposts.*;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.HiringApplicationRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.Employment.EmploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HiringPostServiceImpl implements HiringPostService {

    private final HiringPostRepo hiringPostRepo;
    private final HiringApplicationRepo hiringApplicationRepo;
    private final EmploymentRepo employmentRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final EmploymentService employmentService;

    @Override
    public ApiResponse<HiringPostResponse> createHiringPost(
            CreateHiringPostRequest request,
            String ownerEmail) {

        User owner = userRepo.findByemail(ownerEmail);

        if (owner == null) {
            return ApiResponse.error("Owner not found");
        }

        if (owner.getRole() != UserRole.OWNER) {
            return ApiResponse.error("Only salon owners can create hiring posts");
        }

        Salon salon = salonRepo.findByOwnerEmail(ownerEmail);

        if (salon == null) {
            return ApiResponse.error("Salon not found for this owner");
        }

        if (salon.getStatus() != SalonStatus.Accepted) {
            return ApiResponse.error("Your salon must be accepted before creating a hiring post");
        }

        ApiResponse<Void> validationResponse = validateCreateRequest(request);

        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        List<String> normalizedSkills = request.getRequiredSkills()
                .stream()
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .distinct()
                .toList();

        if (normalizedSkills.isEmpty()) {
            return ApiResponse.error("At least one required skill is needed");
        }

        HiringPost hiringPost = new HiringPost();
        hiringPost.setTitle(request.getTitle().trim());
        hiringPost.setDescription(request.getDescription().trim());
        hiringPost.setRequiredSkills(normalizedSkills);
        hiringPost.setMinimumAge(request.getMinimumAge());
        hiringPost.setMaximumAge(request.getMaximumAge());
        hiringPost.setEmployeeLocation(request.getEmployeeLocation().trim());
        hiringPost.setMinimumYearsOfExperience(request.getMinimumYearsOfExperience());
        hiringPost.setNumOfPositions(request.getNumOfPositions());
        hiringPost.setStatus(HiringPostStatus.Open);
        hiringPost.setCreatedAt(LocalDateTime.now());
        hiringPost.setExpiresAt(request.getExpiresAt());
        hiringPost.setSalon(salon);

        hiringPostRepo.save(hiringPost);

        return ApiResponse.success(
                "Hiring post created successfully",
                mapToResponse(hiringPost)
        );
    }

    @Override
    @Transactional
    public ApiResponse<HiringApplicationResponse> applyToHiringPost(
            Long hiringPostId,
            ApplyToHiringPostRequest request,
            String applicantEmail) {

        Optional<HiringPost> optionalHiringPost = hiringPostRepo.findById(hiringPostId);

        if (optionalHiringPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        User applicant = userRepo.findByemail(applicantEmail);

        if (applicant == null) {
            return ApiResponse.error("Applicant not found");
        }

        if (applicant.getRole() != UserRole.USER
                && applicant.getRole() != UserRole.Employee) {
            return ApiResponse.error("Only users and employees can apply to hiring posts");
        }

        HiringPost hiringPost = optionalHiringPost.get();
        Salon salon = hiringPost.getSalon();

        if (hiringPost.getStatus() != HiringPostStatus.Open) {
            return ApiResponse.error("This hiring post is not open for applications");
        }

        if (!hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
            hiringPost.setStatus(HiringPostStatus.Expired);
            hiringPostRepo.save(hiringPost);
            return ApiResponse.error("This hiring post has expired");
        }

        if (salon.getStatus() != SalonStatus.Accepted) {
            return ApiResponse.error("This salon is not active");
        }

        if (applicant.getSalon() != null
                && applicant.getSalon().getId().equals(salon.getId())) {
            return ApiResponse.error("You are already an employee in this salon");
        }

        if (hiringApplicationRepo.existsByHiringPostAndApplicant(hiringPost, applicant)) {
            return ApiResponse.error("You have already applied to this hiring post");
        }

        ApiResponse<Void> validationResponse = validateApplicationRequest(request);

        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        List<String> normalizedSkills = normalizeSkills(request.getSkills());

        if (normalizedSkills.isEmpty()) {
            return ApiResponse.error("At least one applicant skill is required");
        }

        LocalDateTime appliedAt = LocalDateTime.now();

        EmploymentRequest employmentRequest = new EmploymentRequest();
        employmentRequest.setSalon(salon);
        employmentRequest.setSender(applicant);
        employmentRequest.setReceiver(salon.getOwner());
        employmentRequest.setHiringPost(hiringPost);
        employmentRequest.setStatus(EmploymentRequestStatus.Requested);
        employmentRequest.setRequestType(RequestType.User_Request);
        employmentRequest.setRequestSource(EmploymentRequestSource.HIRING_POST);
        employmentRequest.setCreatedAt(appliedAt);

        employmentRepo.save(employmentRequest);

        HiringApplication hiringApplication = new HiringApplication();
        hiringApplication.setHiringPost(hiringPost);
        hiringApplication.setApplicant(applicant);
        hiringApplication.setEmploymentRequest(employmentRequest);
        hiringApplication.setDateOfBirth(request.getDateOfBirth());
        hiringApplication.setCurrentLocation(request.getCurrentLocation().trim());
        hiringApplication.setYearsOfExperience(request.getYearsOfExperience());
        hiringApplication.setSkills(normalizedSkills);
        hiringApplication.setResumeUrl(normalizeOptionalText(request.getResumeUrl()));
        hiringApplication.setPortfolioUrl(normalizeOptionalText(request.getPortfolioUrl()));
        hiringApplication.setApplicationNote(normalizeOptionalText(request.getApplicationNote()));
        hiringApplication.setCreatedAt(appliedAt);

        hiringApplicationRepo.save(hiringApplication);

        return ApiResponse.success(
                "Application sent successfully",
                mapApplicationToResponse(hiringApplication)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<HiringApplicationResponse>> getHiringPostApplications(
            Long hiringPostId,
            String ownerEmail) {

        Optional<HiringPost> optionalHiringPost = hiringPostRepo.findById(hiringPostId);

        if (optionalHiringPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost hiringPost = optionalHiringPost.get();

        if (!hiringPost.getSalon().getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            return ApiResponse.error("Only the salon owner can view these applications");
        }

        List<HiringApplicationResponse> applications = hiringApplicationRepo
                .findAllByHiringPost(hiringPost)
                .stream()
                .map(this::mapApplicationToResponse)
                .sorted(
                        Comparator.comparingInt(
                                        (HiringApplicationResponse response) ->
                                                matchLevelOrder(response.matchLevel())
                                )
                                .thenComparing(
                                        HiringApplicationResponse::matchScore,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(HiringApplicationResponse::appliedAt)
                )
                .toList();

        return ApiResponse.success(
                "Hiring applications fetched and ranked successfully",
                applications
        );
    }

    @Override
    @Transactional
    public ApiResponse<HiringApplicationResponse> acceptHiringApplication(
            Long applicationId,
            String ownerEmail) {

        Optional<HiringApplication> optionalApplication =
                hiringApplicationRepo.findById(applicationId);

        if (optionalApplication.isEmpty()) {
            return ApiResponse.error("Hiring application not found");
        }

        HiringApplication application = optionalApplication.get();
        HiringPost hiringPost = application.getHiringPost();
        Salon salon = hiringPost.getSalon();
        User owner = userRepo.findByemail(ownerEmail);

        if (owner == null || salon.getOwner().getId() != owner.getId()) {
            return ApiResponse.error("Only the salon owner can accept this application");
        }

        if (hiringPost.getStatus() != HiringPostStatus.Open) {
            return ApiResponse.error("This hiring post is not open");
        }

        if (!hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
            hiringPost.setStatus(HiringPostStatus.Expired);
            hiringPostRepo.save(hiringPost);
            return ApiResponse.error("This hiring post has expired");
        }

        if (hiringPost.getNumOfPositions() <= 0) {
            hiringPost.setStatus(HiringPostStatus.Closed);
            hiringPostRepo.save(hiringPost);
            return ApiResponse.error("This hiring post has no available positions");
        }

        ApiResponse<EmploymentRequest> employmentResponse =
                employmentService.acceptRequest(
                        new AcceptRequest(
                                application.getEmploymentRequest().getId()
                        ),
                        owner.getId()
                );

        if (!employmentResponse.isSuccess()) {
            return ApiResponse.error(employmentResponse.getMessage());
        }

        hiringPost.setNumOfPositions(hiringPost.getNumOfPositions() - 1);

        if (hiringPost.getNumOfPositions() == 0) {
            hiringPost.setStatus(HiringPostStatus.Closed);
        }

        hiringPostRepo.save(hiringPost);

        return ApiResponse.success(
                employmentResponse.getData().getStatus()
                        == EmploymentRequestStatus.Accepted_Pending_Leave
                        ? "Application accepted and transfer leave request created"
                        : "Application accepted and applicant joined the salon",
                mapApplicationToResponse(application)
        );
    }

    @Override
    @Transactional
    public ApiResponse<HiringApplicationResponse> rejectHiringApplication(
            Long applicationId,
            RejectHiringApplicationRequest request,
            String ownerEmail) {

        Optional<HiringApplication> optionalApplication =
                hiringApplicationRepo.findById(applicationId);

        if (optionalApplication.isEmpty()) {
            return ApiResponse.error("Hiring application not found");
        }

        HiringApplication application = optionalApplication.get();
        Salon salon = application.getHiringPost().getSalon();
        User owner = userRepo.findByemail(ownerEmail);

        if (owner == null || salon.getOwner().getId() != owner.getId()) {
            return ApiResponse.error("Only the salon owner can reject this application");
        }

        if (request == null
                || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()) {
            return ApiResponse.error("Rejection reason is required");
        }

        if (request.getRejectionReason().trim().length() > 1000) {
            return ApiResponse.error("Rejection reason cannot exceed 1000 characters");
        }

        ApiResponse<EmploymentRequest> employmentResponse =
                employmentService.rejectRequest(
                        new RejectRequest(
                                application.getEmploymentRequest().getId(),
                                request.getRejectionReason().trim()
                        ),
                        owner.getId()
                );

        if (!employmentResponse.isSuccess()) {
            return ApiResponse.error(employmentResponse.getMessage());
        }

        return ApiResponse.success(
                "Hiring application rejected successfully",
                mapApplicationToResponse(application)
        );
    }

    private ApiResponse<Void> validateCreateRequest(CreateHiringPostRequest request) {

        if (request == null) {
            return ApiResponse.error("Hiring post information is required");
        }

        if (request.getTitle() == null || request.getTitle().trim().length() < 3) {
            return ApiResponse.error("Title is required and must contain at least 3 characters");
        }

        if (request.getDescription() == null || request.getDescription().trim().length() < 10) {
            return ApiResponse.error("Description is required and must contain at least 10 characters");
        }

        if (request.getRequiredSkills() == null || request.getRequiredSkills().isEmpty()) {
            return ApiResponse.error("At least one required skill is needed");
        }

        if (request.getMinimumAge() == null || request.getMinimumAge() < 18) {
            return ApiResponse.error("Minimum age is required and cannot be less than 18");
        }

        if (request.getMaximumAge() == null
                || request.getMaximumAge() < request.getMinimumAge()) {
            return ApiResponse.error("Maximum age is required and must be greater than or equal to minimum age");
        }

        if (request.getEmployeeLocation() == null || request.getEmployeeLocation().isBlank()) {
            return ApiResponse.error("Required employee location is required");
        }

        if (request.getMinimumYearsOfExperience() == null
                || request.getMinimumYearsOfExperience() < 0) {
            return ApiResponse.error("Minimum years of experience is required and cannot be negative");
        }

        if (request.getNumOfPositions() == null || request.getNumOfPositions() <= 0) {
            return ApiResponse.error("Number of positions is required and must be greater than 0");
        }

        if (request.getExpiresAt() == null
                || !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return ApiResponse.error("Expiration date is required and must be in the future");
        }

        return ApiResponse.success("Hiring post information is valid", null);
    }

    private ApiResponse<Void> validateApplicationRequest(ApplyToHiringPostRequest request) {

        if (request == null) {
            return ApiResponse.error("Application information is required");
        }

        if (request.getDateOfBirth() == null
                || !request.getDateOfBirth().isBefore(LocalDate.now())) {
            return ApiResponse.error("A valid date of birth is required");
        }

        int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();

        if (age < 18) {
            return ApiResponse.error("Applicant must be at least 18 years old");
        }

        if (request.getCurrentLocation() == null || request.getCurrentLocation().isBlank()) {
            return ApiResponse.error("Current location is required");
        }

        if (request.getYearsOfExperience() == null
                || request.getYearsOfExperience() < 0) {
            return ApiResponse.error("Years of experience is required and cannot be negative");
        }

        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            return ApiResponse.error("At least one applicant skill is required");
        }

        if (request.getApplicationNote() != null
                && request.getApplicationNote().length() > 2000) {
            return ApiResponse.error("Application note cannot exceed 2000 characters");
        }

        return ApiResponse.success("Application information is valid", null);
    }

    private List<String> normalizeSkills(List<String> skills) {

        Map<String, String> normalizedSkills = new LinkedHashMap<>();

        for (String skill : skills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }

            String trimmedSkill = skill.trim();
            normalizedSkills.putIfAbsent(
                    trimmedSkill.toLowerCase(Locale.ROOT),
                    trimmedSkill
            );
        }

        return new ArrayList<>(normalizedSkills.values());
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HiringApplicationResponse mapApplicationToResponse(
            HiringApplication application) {

        HiringPost hiringPost = application.getHiringPost();
        Salon salon = hiringPost.getSalon();
        User applicant = application.getApplicant();
        MatchResult matchResult = calculateMatch(application);
        int age = Period.between(
                application.getDateOfBirth(),
                LocalDate.now()
        ).getYears();

        return new HiringApplicationResponse(
                application.getId(),
                application.getEmploymentRequest().getId(),
                hiringPost.getId(),
                hiringPost.getTitle(),
                salon.getId(),
                salon.getName(),
                applicant.getId(),
                applicant.getFirstName() + " " + applicant.getLastName(),
                applicant.getUserName(),
                applicant.getEmail(),
                applicant.getPhoneNumber(),
                application.getDateOfBirth(),
                age,
                application.getCurrentLocation(),
                application.getYearsOfExperience(),
                application.getSkills(),
                application.getResumeUrl(),
                application.getResumeUrl() != null,
                application.getPortfolioUrl(),
                application.getApplicationNote(),
                matchResult.level(),
                matchResult.score(),
                matchResult.matchedSkills(),
                matchResult.additionalSkills(),
                matchResult.missingSkills(),
                matchResult.missingRequirements(),
                application.getEmploymentRequest().getStatus(),
                application.getCreatedAt()
        );
    }

    private MatchResult calculateMatch(HiringApplication application) {

        HiringPost hiringPost = application.getHiringPost();
        int age = Period.between(
                application.getDateOfBirth(),
                LocalDate.now()
        ).getYears();

        Map<String, String> requiredSkills = toNormalizedSkillMap(
                hiringPost.getRequiredSkills()
        );
        Map<String, String> applicantSkills = toNormalizedSkillMap(
                application.getSkills()
        );

        List<String> matchedSkills = requiredSkills.entrySet()
                .stream()
                .filter(entry -> applicantSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        List<String> missingSkills = requiredSkills.entrySet()
                .stream()
                .filter(entry -> !applicantSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        List<String> additionalSkills = applicantSkills.entrySet()
                .stream()
                .filter(entry -> !requiredSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        boolean experienceMatched = application.getYearsOfExperience()
                >= hiringPost.getMinimumYearsOfExperience();
        boolean ageMatched = age >= hiringPost.getMinimumAge()
                && age <= hiringPost.getMaximumAge();
        boolean locationMatched = application.getCurrentLocation().trim()
                .equalsIgnoreCase(hiringPost.getEmployeeLocation().trim());
        boolean allRequiredSkillsMatched = missingSkills.isEmpty();

        List<String> missingRequirements = new ArrayList<>();

        if (!missingSkills.isEmpty()) {
            missingRequirements.add(
                    "Missing required skills: " + String.join(", ", missingSkills)
            );
        }

        if (!experienceMatched) {
            missingRequirements.add(
                    "Needs " + hiringPost.getMinimumYearsOfExperience()
                            + " years of experience; applicant has "
                            + application.getYearsOfExperience()
            );
        }

        if (!ageMatched) {
            missingRequirements.add(
                    "Required age is " + hiringPost.getMinimumAge()
                            + " to " + hiringPost.getMaximumAge()
                            + "; applicant age is " + age
            );
        }

        if (!locationMatched) {
            missingRequirements.add(
                    "Required location is " + hiringPost.getEmployeeLocation()
                            + "; applicant location is "
                            + application.getCurrentLocation()
            );
        }

        boolean allRequirementsMatched = allRequiredSkillsMatched
                && experienceMatched
                && ageMatched
                && locationMatched;

        boolean exceedsExperience = application.getYearsOfExperience()
                > hiringPost.getMinimumYearsOfExperience();
        boolean hasAdditionalSkills = !additionalSkills.isEmpty();

        ApplicationMatchLevel level;

        if (allRequirementsMatched && exceedsExperience && hasAdditionalSkills) {
            level = ApplicationMatchLevel.SUGGESTED;
        } else if (allRequirementsMatched) {
            level = ApplicationMatchLevel.MATCHED;
        } else {
            level = ApplicationMatchLevel.MISSING_REQUIREMENTS;
        }

        int score = calculateMatchScore(
                matchedSkills.size(),
                requiredSkills.size(),
                additionalSkills.size(),
                application.getYearsOfExperience(),
                hiringPost.getMinimumYearsOfExperience(),
                ageMatched,
                locationMatched,
                application.getResumeUrl() != null
        );

        return new MatchResult(
                level,
                score,
                matchedSkills,
                additionalSkills,
                missingSkills,
                missingRequirements
        );
    }

    private int calculateMatchScore(
            int matchedSkillCount,
            int requiredSkillCount,
            int additionalSkillCount,
            int applicantExperience,
            int requiredExperience,
            boolean ageMatched,
            boolean locationMatched,
            boolean resumeUploaded) {

        double skillCoverage = requiredSkillCount == 0
                ? 1
                : (double) matchedSkillCount / requiredSkillCount;
        double experienceCoverage = requiredExperience == 0
                ? 1
                : Math.min(1, (double) applicantExperience / requiredExperience);

        int skillScore = (int) Math.round(skillCoverage * 50);
        int experienceScore = (int) Math.round(experienceCoverage * 25);
        int ageScore = ageMatched ? 10 : 0;
        int locationScore = locationMatched ? 10 : 0;
        int additionalSkillScore = Math.min(5, additionalSkillCount);
        int resumeScore = resumeUploaded ? 5 : 0;

        return Math.min(100, skillScore
                + experienceScore
                + ageScore
                + locationScore
                + additionalSkillScore
                + resumeScore);
    }

    private Map<String, String> toNormalizedSkillMap(List<String> skills) {

        Map<String, String> normalizedSkills = new LinkedHashMap<>();

        for (String skill : skills) {
            normalizedSkills.putIfAbsent(
                    skill.trim().toLowerCase(Locale.ROOT),
                    skill.trim()
            );
        }

        return normalizedSkills;
    }

    private int matchLevelOrder(ApplicationMatchLevel level) {
        return switch (level) {
            case SUGGESTED -> 0;
            case MATCHED -> 1;
            case MISSING_REQUIREMENTS -> 2;
        };
    }

    private record MatchResult(
            ApplicationMatchLevel level,
            Integer score,
            List<String> matchedSkills,
            List<String> additionalSkills,
            List<String> missingSkills,
            List<String> missingRequirements
    ) {
    }

    private HiringPostResponse mapToResponse(HiringPost hiringPost) {

        Salon salon = hiringPost.getSalon();
        User owner = salon.getOwner();

        return new HiringPostResponse(
                hiringPost.getId(),
                hiringPost.getTitle(),
                hiringPost.getDescription(),
                hiringPost.getRequiredSkills(),
                hiringPost.getMinimumAge(),
                hiringPost.getMaximumAge(),
                hiringPost.getEmployeeLocation(),
                hiringPost.getMinimumYearsOfExperience(),
                hiringPost.getNumOfPositions(),
                hiringPost.getStatus(),
                hiringPost.getCreatedAt(),
                hiringPost.getExpiresAt(),
                salon.getId(),
                salon.getName(),
                salon.getLocation(),
                owner.getId(),
                owner.getFirstName() + " " + owner.getLastName()
        );
    }
}
