package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.Services.Subscription.PlanLimits;
import com.salon.backend.Services.Email.WorkforceEmailNotifier;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostDiscoveryResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostPageResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostSearchRequest;
import com.salon.backend.DTOs.Salon.HiringPost.UpdateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ReopenHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.OwnerHiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;
import com.salon.backend.DTOs.Salon.Employment.Join.AcceptRequest;
import com.salon.backend.DTOs.Salon.Employment.Join.EmployeeRequestResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.RejectRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.EmploymentRequestSource;
import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.salons.employment.LeaveRequestStatus;
import com.salon.backend.Entities.salons.employment.RequestType;
import com.salon.backend.Entities.salons.hiringposts.*;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.HiringApplicationRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.LeaveRequestRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.Employment.EmploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

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
    private final LeaveRequestRepo leaveRequestRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final EmploymentService employmentService;
    private final WorkforceEmailNotifier workforceEmails;
    private final PlanLimits planLimits;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<HiringPostPageResponse> searchOpenHiringPosts(
            HiringPostSearchRequest request,
            Long userId) {

        ApiResponse<Void> validationResponse = validateSearchRequest(request);

        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        int pageNumber = request.getPage() == null ? 0 : request.getPage();
        int pageSize = request.getSize() == null ? 10 : request.getSize();
        Sort sort = resolveSearchSort(request.getSortBy());
        Specification<HiringPost> specification = buildOpenPostSpecification(request);

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error("User not found");
        }

        HiringPostPageResponse response;
        if (hasUsableCareerProfile(user)) {
            List<HiringPostDiscoveryResponse> rankedPosts = hiringPostRepo
                    .findAll(specification, sort)
                    .stream()
                    .map(post -> mapToDiscoveryResponse(post, user))
                    .sorted(personalizedPostComparator(request.getSortBy()))
                    .toList();

            response = createInMemoryPage(rankedPosts, pageNumber, pageSize);
            response.setPersonalized(true);
            response.setPersonalizationMessage(
                    "Posts are ranked using your career skills, experience, age, and preferred locations"
            );
        } else {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
            Page<HiringPost> postPage = hiringPostRepo.findAll(specification, pageable);
            response = new HiringPostPageResponse(
                    postPage.getContent().stream()
                            .map(this::mapToUnrankedDiscoveryResponse)
                            .toList(),
                    postPage.getNumber(),
                    postPage.getSize(),
                    postPage.getTotalElements(),
                    postPage.getTotalPages(),
                    postPage.isFirst(),
                    postPage.isLast(),
                    false,
                    "Enable career mode to receive personalized hiring-post suggestions"
            );
        }

        return ApiResponse.success("Open hiring posts fetched successfully", response);
    }

    @Override
    public ApiResponse<HiringPostResponse> createHiringPost(
            CreateHiringPostRequest request,
            Long ownerId) {

        User owner = userRepo.findById(ownerId).orElse(null);

        if (owner == null) {
            return ApiResponse.error("Owner not found");
        }

        if (owner.getRole() != UserRole.OWNER) {
            return ApiResponse.error("Only salon owners can create hiring posts");
        }

        Salon salon = salonRepo.findByOwnerId(ownerId);

        if (salon == null) {
            return ApiResponse.error("Salon not found for this owner");
        }

        if (salon.getStatus() != SalonStatus.Accepted) {
            return ApiResponse.error("Your salon must be accepted before creating a hiring post");
        }

        String postLimitError = planLimits.hiringPostLimitError(salon);
        if (postLimitError != null) {
            return ApiResponse.error(postLimitError);
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
    public ApiResponse<List<OwnerHiringPostResponse>> getOwnerHiringPosts(
            HiringPostStatus status,
            Long ownerId) {

        Salon salon = findOwnedSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error("Salon not found for this owner");
        }

        List<HiringPost> posts = status == null
                ? hiringPostRepo.findAllBySalonOrderByCreatedAtDesc(salon)
                : hiringPostRepo.findAllBySalonAndStatusOrderByCreatedAtDesc(salon, status);

        List<HiringPost> refreshedPosts = posts.stream()
                .map(this::refreshExpiration)
                .filter(post -> status == null || post.getStatus() == status)
                .toList();
        Map<Long, List<HiringApplication>> applicationsByPost = refreshedPosts.isEmpty()
                ? Map.of()
                : hiringApplicationRepo.findAllByHiringPostIn(refreshedPosts)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        application -> application.getHiringPost().getId()
                ));

        List<OwnerHiringPostResponse> response = refreshedPosts.stream()
                .map(post -> mapToOwnerResponse(
                        post,
                        applicationsByPost.getOrDefault(post.getId(), List.of())
                ))
                .toList();

        return ApiResponse.success("Owner hiring posts fetched successfully", response);
    }

    @Override
    @Transactional
    public ApiResponse<OwnerHiringPostResponse> getOwnerHiringPost(
            Long hiringPostId,
            Long ownerId) {

        Optional<HiringPost> optionalPost = hiringPostRepo.findById(hiringPostId);
        if (optionalPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost post = optionalPost.get();
        if (!isOwnedBy(post, ownerId)) {
            return ApiResponse.error("Only the salon owner can inspect this hiring post");
        }

        return ApiResponse.success(
                "Owner hiring post fetched successfully",
                mapToOwnerResponse(refreshExpiration(post))
        );
    }

    @Override
    @Transactional
    public ApiResponse<HiringPostResponse> updateHiringPost(
            Long hiringPostId,
            UpdateHiringPostRequest request,
            Long ownerId) {

        Optional<HiringPost> optionalPost = hiringPostRepo.findByIdForUpdate(hiringPostId);
        if (optionalPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost post = refreshExpiration(optionalPost.get());
        if (!isOwnedBy(post, ownerId)) {
            return ApiResponse.error("Only the salon owner can update this hiring post");
        }

        ApiResponse<Void> validationResponse = validateUpdateRequest(request);
        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        if (request.getExpiresAt() != null
                && post.getStatus() == HiringPostStatus.Expired) {
            return ApiResponse.error("Use the reopen action to extend an expired post");
        }

        if (request.getAdditionalPositions() != null
                && (long) post.getNumOfPositions()
                + request.getAdditionalPositions() > Integer.MAX_VALUE) {
            return ApiResponse.error("Resulting available positions value is too large");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            post.setDescription(request.getDescription().trim());
        }
        if (request.getExpiresAt() != null) {
            post.setExpiresAt(request.getExpiresAt());
        }
        if (request.getAdditionalPositions() != null
                && request.getAdditionalPositions() > 0) {
            post.setNumOfPositions(Math.addExact(
                    post.getNumOfPositions(),
                    request.getAdditionalPositions()
            ));
        }

        hiringPostRepo.save(post);
        return ApiResponse.success("Hiring post updated successfully", mapToResponse(post));
    }

    @Override
    @Transactional
    public ApiResponse<HiringPostResponse> closeHiringPost(
            Long hiringPostId,
            Long ownerId) {

        Optional<HiringPost> optionalPost = hiringPostRepo.findByIdForUpdate(hiringPostId);
        if (optionalPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost post = refreshExpiration(optionalPost.get());
        if (!isOwnedBy(post, ownerId)) {
            return ApiResponse.error("Only the salon owner can close this hiring post");
        }
        if (post.getStatus() != HiringPostStatus.Open) {
            return ApiResponse.error("Only an open hiring post can be closed");
        }

        post.setStatus(HiringPostStatus.Closed);
        post.setClosureReason(HiringPostClosureReason.MANUALLY_CLOSED);
        hiringPostRepo.save(post);
        return ApiResponse.success("Hiring post closed successfully", mapToResponse(post));
    }

    @Override
    @Transactional
    public ApiResponse<HiringPostResponse> reopenHiringPost(
            Long hiringPostId,
            ReopenHiringPostRequest request,
            Long ownerId) {

        Optional<HiringPost> optionalPost = hiringPostRepo.findByIdForUpdate(hiringPostId);
        if (optionalPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost post = refreshExpiration(optionalPost.get());
        if (!isOwnedBy(post, ownerId)) {
            return ApiResponse.error("Only the salon owner can reopen this hiring post");
        }
        if (post.getStatus() == HiringPostStatus.Open) {
            return ApiResponse.error("This hiring post is already open");
        }
        String postLimitError = planLimits.hiringPostLimitError(post.getSalon());
        if (postLimitError != null) {
            return ApiResponse.error(postLimitError);
        }

        ApiResponse<Void> validationResponse = validateReopenRequest(request);
        if (!validationResponse.isSuccess()) {
            return ApiResponse.error(validationResponse.getMessage());
        }

        int additionalPositions = request.getAdditionalPositions() == null
                ? 0
                : request.getAdditionalPositions();
        long resultingPositions = (long) post.getNumOfPositions()
                + additionalPositions;
        if (resultingPositions > Integer.MAX_VALUE) {
            return ApiResponse.error("Resulting available positions value is too large");
        }
        int availablePositions = (int) resultingPositions;
        if (availablePositions <= 0) {
            return ApiResponse.error(
                    "Add at least one position because this post has no restored availability"
            );
        }

        post.setNumOfPositions(availablePositions);
        post.setExpiresAt(request.getExpiresAt());
        post.setStatus(HiringPostStatus.Open);
        post.setClosureReason(null);
        hiringPostRepo.save(post);

        return ApiResponse.success("Hiring post reopened successfully", mapToResponse(post));
    }

    @Override
    @Transactional
    public ApiResponse<HiringApplicationResponse> applyToHiringPost(
            Long hiringPostId,
            ApplyToHiringPostRequest request,
            Long applicantId) {
        var result = applyToHiringPostInternal(hiringPostId, request, applicantId);
        workforceEmails.applicationReceived(result);
        return result;
    }

    private ApiResponse<HiringApplicationResponse> applyToHiringPostInternal(
            Long hiringPostId,
            ApplyToHiringPostRequest request,
            Long applicantId) {

        Optional<HiringPost> optionalHiringPost = hiringPostRepo.findById(hiringPostId);

        if (optionalHiringPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        User applicant = userRepo.findById(applicantId).orElse(null);

        if (applicant == null) {
            return ApiResponse.error("Applicant not found");
        }

        if (applicant.getRole() != UserRole.USER
                && applicant.getRole() != UserRole.Employee) {
            return ApiResponse.error("Only users and employees can apply to hiring posts");
        }

        if (!hasUsableCareerProfile(applicant)) {
            return ApiResponse.error(
                    "Enable career mode and complete your career information before applying"
            );
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
        hiringApplication.setDateOfBirth(applicant.getDateOfBirth());
        hiringApplication.setYearsOfExperience(applicant.getYearsOfExperience());
        hiringApplication.setSkills(List.copyOf(applicant.getCareerSkills()));
        hiringApplication.setPreferredWorkLocations(
                List.copyOf(applicant.getPreferredWorkLocations())
        );
        hiringApplication.setResumeUrl(applicant.getResumeUrl());
        hiringApplication.setPortfolioUrl(applicant.getPortfolioUrl());
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
            Long ownerId) {

        Optional<HiringPost> optionalHiringPost = hiringPostRepo.findById(hiringPostId);

        if (optionalHiringPost.isEmpty()) {
            return ApiResponse.error("Hiring post not found");
        }

        HiringPost hiringPost = optionalHiringPost.get();

        if (hiringPost.getSalon().getOwner().getId() != ownerId) {
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
            Long ownerId) {

        Optional<HiringApplication> optionalApplication =
                hiringApplicationRepo.findByIdForUpdate(applicationId);

        if (optionalApplication.isEmpty()) {
            return ApiResponse.error("Hiring application not found");
        }

        HiringApplication application = optionalApplication.get();
        HiringPost hiringPost = hiringPostRepo.findByIdForUpdate(
                application.getHiringPost().getId()
        ).orElse(application.getHiringPost());
        Salon salon = salonRepo.findByIdForUpdate(hiringPost.getSalon().getId())
                .orElse(hiringPost.getSalon());
        User owner = userRepo.findById(ownerId).orElse(null);

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
            hiringPost.setClosureReason(HiringPostClosureReason.POSITIONS_FILLED);
            hiringPostRepo.save(hiringPost);
            return ApiResponse.error("This hiring post has no available positions");
        }

        ApiResponse<EmployeeRequestResponse> employmentResponse =
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
            hiringPost.setClosureReason(HiringPostClosureReason.POSITIONS_FILLED);
        }

        hiringPostRepo.save(hiringPost);

        return ApiResponse.success(
                employmentResponse.getData().status()
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
            Long ownerId) {

        Optional<HiringApplication> optionalApplication =
                hiringApplicationRepo.findByIdForUpdate(applicationId);

        if (optionalApplication.isEmpty()) {
            return ApiResponse.error("Hiring application not found");
        }

        HiringApplication application = optionalApplication.get();
        Salon salon = application.getHiringPost().getSalon();
        User owner = userRepo.findById(ownerId).orElse(null);

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

        ApiResponse<EmployeeRequestResponse> employmentResponse =
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

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<HiringApplicationResponse>> getMyHiringApplications(
            Long applicantId) {

        User applicant = userRepo.findById(applicantId).orElse(null);
        if (applicant == null) {
            return ApiResponse.error("Applicant not found");
        }

        List<HiringApplicationResponse> applications = hiringApplicationRepo
                .findAllByApplicantOrderByCreatedAtDesc(applicant)
                .stream()
                .map(this::mapApplicationToResponse)
                .toList();

        return ApiResponse.success(
                "Hiring application history fetched successfully",
                applications
        );
    }

    @Override
    @Transactional
    public ApiResponse<HiringApplicationResponse> cancelLinkedJoinRequest(
            Long applicationId,
            Long applicantId) {

        Optional<HiringApplication> optionalApplication =
                hiringApplicationRepo.findByIdForUpdate(applicationId);
        if (optionalApplication.isEmpty()) {
            return ApiResponse.error("Hiring application not found");
        }

        HiringApplication application = optionalApplication.get();
        if (application.getApplicant().getId() != applicantId) {
            return ApiResponse.error(
                    "Only the applicant can cancel the linked join request"
            );
        }

        EmploymentRequest employmentRequest = employmentRepo.findByIdForUpdate(
                application.getEmploymentRequest().getId()
        ).orElse(application.getEmploymentRequest());
        EmploymentRequestStatus status = employmentRequest.getStatus();
        if (status != EmploymentRequestStatus.Requested
                && status != EmploymentRequestStatus.Accepted_Pending_Leave) {
            return ApiResponse.error(
                    "Only a requested or pending-transfer join request can be cancelled"
            );
        }

        if (status == EmploymentRequestStatus.Accepted_Pending_Leave) {
            leaveRequestRepo.findByEmploymentRequest(employmentRequest)
                    .filter(leaveRequest ->
                            leaveRequest.getStatus() == LeaveRequestStatus.Requested)
                    .ifPresent(leaveRequest -> {
                        leaveRequest.setStatus(LeaveRequestStatus.Cancelled);
                        leaveRequestRepo.save(leaveRequest);
                    });

            HiringPost hiringPost = hiringPostRepo.findByIdForUpdate(
                    application.getHiringPost().getId()
            ).orElse(application.getHiringPost());
            hiringPost.setNumOfPositions(hiringPost.getNumOfPositions() + 1);

            if (hiringPost.getStatus() == HiringPostStatus.Closed
                    && hiringPost.getClosureReason()
                    == HiringPostClosureReason.POSITIONS_FILLED
                    && hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
                hiringPost.setStatus(HiringPostStatus.Open);
                hiringPost.setClosureReason(null);
            }

            hiringPostRepo.save(hiringPost);
        }

        employmentRequest.setStatus(EmploymentRequestStatus.Cancelled);
        employmentRepo.save(employmentRequest);

        return ApiResponse.success(
                "Linked join request cancelled; application retained in history",
                mapApplicationToResponse(application)
        );
    }

    private Salon findOwnedSalon(Long ownerId) {
        User owner = userRepo.findById(ownerId).orElse(null);
        if (owner == null || owner.getRole() != UserRole.OWNER) {
            return null;
        }
        return salonRepo.findByOwnerId(ownerId);
    }

    private boolean isOwnedBy(HiringPost hiringPost, Long ownerId) {
        return hiringPost.getSalon() != null
                && hiringPost.getSalon().getOwner() != null
                && hiringPost.getSalon().getOwner().getId() == ownerId;
    }

    private HiringPost refreshExpiration(HiringPost hiringPost) {
        if (hiringPost.getStatus() == HiringPostStatus.Open
                && !hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
            hiringPost.setStatus(HiringPostStatus.Expired);
            hiringPost.setClosureReason(null);
            return hiringPostRepo.save(hiringPost);
        }
        return hiringPost;
    }

    private OwnerHiringPostResponse mapToOwnerResponse(HiringPost hiringPost) {
        return mapToOwnerResponse(
                hiringPost,
                hiringApplicationRepo.findAllByHiringPost(hiringPost)
        );
    }

    private OwnerHiringPostResponse mapToOwnerResponse(
            HiringPost hiringPost,
            List<HiringApplication> applications) {

        long requested = countApplicationsByStatus(
                applications,
                EmploymentRequestStatus.Requested
        );
        long pendingTransfer = countApplicationsByStatus(
                applications,
                EmploymentRequestStatus.Accepted_Pending_Leave
        );
        long accepted = countApplicationsByStatus(
                applications,
                EmploymentRequestStatus.Accepted
        );
        long rejected = countApplicationsByStatus(
                applications,
                EmploymentRequestStatus.Rejected
        );
        long cancelled = countApplicationsByStatus(
                applications,
                EmploymentRequestStatus.Cancelled
        );

        return new OwnerHiringPostResponse(
                mapToResponse(hiringPost),
                (long) applications.size(),
                requested,
                pendingTransfer,
                accepted,
                rejected,
                cancelled
        );
    }

    private long countApplicationsByStatus(
            List<HiringApplication> applications,
            EmploymentRequestStatus status) {
        return applications.stream()
                .filter(application ->
                        application.getEmploymentRequest().getStatus() == status)
                .count();
    }

    private ApiResponse<Void> validateUpdateRequest(UpdateHiringPostRequest request) {
        if (request == null) {
            return ApiResponse.error("Hiring post update information is required");
        }

        if (request.getTitle() == null
                && request.getDescription() == null
                && request.getExpiresAt() == null
                && request.getAdditionalPositions() == null) {
            return ApiResponse.error("At least one editable field is required");
        }

        if (request.getTitle() != null && request.getTitle().trim().length() < 3) {
            return ApiResponse.error("Title must contain at least 3 characters");
        }
        if (request.getDescription() != null
                && request.getDescription().trim().length() < 10) {
            return ApiResponse.error("Description must contain at least 10 characters");
        }
        if (request.getExpiresAt() != null
                && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return ApiResponse.error("Expiration date must be in the future");
        }
        if (request.getAdditionalPositions() != null
                && request.getAdditionalPositions() < 0) {
            return ApiResponse.error("Additional positions cannot be negative");
        }
        if (request.getAdditionalPositions() != null
                && request.getAdditionalPositions() > Integer.MAX_VALUE / 2) {
            return ApiResponse.error("Additional positions value is too large");
        }

        return ApiResponse.success("Hiring post update is valid", null);
    }

    private ApiResponse<Void> validateReopenRequest(ReopenHiringPostRequest request) {
        if (request == null || request.getExpiresAt() == null) {
            return ApiResponse.error("A new expiration date is required");
        }
        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return ApiResponse.error("The new expiration date must be in the future");
        }
        if (request.getAdditionalPositions() != null
                && request.getAdditionalPositions() < 0) {
            return ApiResponse.error("Additional positions cannot be negative");
        }
        if (request.getAdditionalPositions() != null
                && request.getAdditionalPositions() > Integer.MAX_VALUE / 2) {
            return ApiResponse.error("Additional positions value is too large");
        }

        return ApiResponse.success("Hiring post reopen information is valid", null);
    }

    private ApiResponse<Void> validateSearchRequest(HiringPostSearchRequest request) {

        if (request == null) {
            return ApiResponse.error("Search information is required");
        }

        if (request.getPage() != null && request.getPage() < 0) {
            return ApiResponse.error("Page number cannot be negative");
        }

        if (request.getSize() != null
                && (request.getSize() < 1 || request.getSize() > 50)) {
            return ApiResponse.error("Page size must be between 1 and 50");
        }

        if (request.getMaximumRequiredExperience() != null
                && request.getMaximumRequiredExperience() < 0) {
            return ApiResponse.error("Maximum required experience cannot be negative");
        }

        String sortBy = normalizeOptionalText(request.getSortBy());

        if (sortBy != null
                && !Set.of("newest", "expiringSoon", "experienceAscending")
                .contains(sortBy)) {
            return ApiResponse.error(
                    "Sort option must be newest, expiringSoon, or experienceAscending"
            );
        }

        return ApiResponse.success("Hiring-post search information is valid", null);
    }

    private Specification<HiringPost> buildOpenPostSpecification(
            HiringPostSearchRequest request) {

        LocalDateTime now = LocalDateTime.now();
        Specification<HiringPost> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(
                                root.get("status"),
                                HiringPostStatus.Open
                        ),
                        criteriaBuilder.greaterThan(root.get("expiresAt"), now),
                        criteriaBuilder.greaterThan(root.get("numOfPositions"), 0),
                        criteriaBuilder.equal(
                                root.join("salon", JoinType.INNER).get("status"),
                                SalonStatus.Accepted
                        )
                );

        String keyword = normalizeOptionalText(request.getKeyword());
        if (keyword != null) {
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("title")),
                                    pattern
                            ),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("description")),
                                    pattern
                            ),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.join("salon", JoinType.INNER).get("name")
                                    ),
                                    pattern
                            )
                    ));
        }

        String location = normalizeOptionalText(request.getEmployeeLocation());
        if (location != null) {
            String pattern = "%" + location.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("employeeLocation")),
                            pattern
                    ));
        }

        String requiredSkill = normalizeOptionalText(request.getRequiredSkill());
        if (requiredSkill != null) {
            specification = specification.and((root, query, criteriaBuilder) -> {
                query.distinct(true);
                return criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                root.join("requiredSkills", JoinType.INNER)
                        ),
                        requiredSkill.toLowerCase(Locale.ROOT)
                );
            });
        }

        if (request.getMaximumRequiredExperience() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(
                            root.get("minimumYearsOfExperience"),
                            request.getMaximumRequiredExperience()
                    ));
        }

        return specification;
    }

    private Sort resolveSearchSort(String requestedSort) {
        String sortBy = normalizeOptionalText(requestedSort);

        if ("expiringSoon".equals(sortBy)) {
            return Sort.by(
                    Sort.Order.asc("expiresAt"),
                    Sort.Order.desc("createdAt")
            );
        }

        if ("experienceAscending".equals(sortBy)) {
            return Sort.by(
                    Sort.Order.asc("minimumYearsOfExperience"),
                    Sort.Order.desc("createdAt")
            );
        }

        return Sort.by(Sort.Order.desc("createdAt"));
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
                salon.getLocation(),
                applicant.getId(),
                applicant.getFirstName() + " " + applicant.getLastName(),
                applicant.getUserName(),
                applicant.getEmail(),
                applicant.getPhoneNumber(),
                application.getDateOfBirth(),
                age,
                application.getPreferredWorkLocations(),
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

        return calculateMatch(
                application.getHiringPost(),
                application.getDateOfBirth(),
                application.getPreferredWorkLocations(),
                application.getYearsOfExperience(),
                application.getSkills(),
                application.getResumeUrl() != null
        );
    }

    private MatchResult calculateMatch(
            HiringPost hiringPost,
            LocalDate dateOfBirth,
            List<String> preferredWorkLocations,
            Integer yearsOfExperience,
            List<String> skills,
            boolean resumeUploaded) {

        int age = Period.between(
                dateOfBirth,
                LocalDate.now()
        ).getYears();

        Map<String, String> requiredSkills = toNormalizedSkillMap(
                hiringPost.getRequiredSkills()
        );
        Map<String, String> applicantSkills = toNormalizedSkillMap(
                skills
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

        boolean experienceMatched = yearsOfExperience
                >= hiringPost.getMinimumYearsOfExperience();
        boolean ageMatched = age >= hiringPost.getMinimumAge()
                && age <= hiringPost.getMaximumAge();
        boolean locationMatched = containsIgnoreCase(
                preferredWorkLocations,
                hiringPost.getEmployeeLocation()
        );
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
                            + yearsOfExperience
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
                            + "; applicant preferred locations are "
                            + String.join(", ", preferredWorkLocations)
            );
        }

        boolean allRequirementsMatched = allRequiredSkillsMatched
                && experienceMatched
                && ageMatched
                && locationMatched;

        boolean exceedsExperience = yearsOfExperience
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
                yearsOfExperience,
                hiringPost.getMinimumYearsOfExperience(),
                ageMatched,
                locationMatched,
                resumeUploaded
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

    private HiringPostDiscoveryResponse mapToDiscoveryResponse(
            HiringPost hiringPost,
            User user) {

        MatchResult matchResult = calculateMatch(
                hiringPost,
                user.getDateOfBirth(),
                user.getPreferredWorkLocations(),
                user.getYearsOfExperience(),
                user.getCareerSkills(),
                user.getResumeUrl() != null
        );

        return new HiringPostDiscoveryResponse(
                mapToResponse(hiringPost),
                matchResult.level(),
                matchResult.score(),
                matchResult.matchedSkills(),
                matchResult.additionalSkills(),
                matchResult.missingSkills(),
                matchResult.missingRequirements()
        );
    }

    private HiringPostDiscoveryResponse mapToUnrankedDiscoveryResponse(
            HiringPost hiringPost) {
        return new HiringPostDiscoveryResponse(
                mapToResponse(hiringPost),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private Comparator<HiringPostDiscoveryResponse> personalizedPostComparator(
            String requestedSort) {

        Comparator<HiringPostDiscoveryResponse> comparator = Comparator
                .comparingInt(
                        (HiringPostDiscoveryResponse response) ->
                                matchLevelOrder(response.matchLevel())
                )
                .thenComparing(
                        HiringPostDiscoveryResponse::matchScore,
                        Comparator.reverseOrder()
                );

        String sortBy = normalizeOptionalText(requestedSort);
        if ("expiringSoon".equals(sortBy)) {
            return comparator.thenComparing(
                    response -> response.hiringPost().getExpiresAt()
            );
        }

        if ("experienceAscending".equals(sortBy)) {
            return comparator.thenComparing(
                    response -> response.hiringPost().getMinimumYearsOfExperience()
            );
        }

        return comparator.thenComparing(
                response -> response.hiringPost().getCreatedAt(),
                Comparator.reverseOrder()
        );
    }

    private HiringPostPageResponse createInMemoryPage(
            List<HiringPostDiscoveryResponse> rankedPosts,
            int pageNumber,
            int pageSize) {

        int totalElements = rankedPosts.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / pageSize);
        long requestedOffset = (long) pageNumber * pageSize;
        int fromIndex = (int) Math.min(requestedOffset, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        return new HiringPostPageResponse(
                rankedPosts.subList(fromIndex, toIndex),
                pageNumber,
                pageSize,
                (long) totalElements,
                totalPages,
                pageNumber == 0,
                totalPages == 0 || pageNumber >= totalPages - 1,
                true,
                null
        );
    }

    private boolean hasUsableCareerProfile(User user) {
        return user.isCareerProfileEnabled()
                && user.getDateOfBirth() != null
                && user.getYearsOfExperience() != null
                && user.getCareerSkills() != null
                && !user.getCareerSkills().isEmpty()
                && user.getPreferredWorkLocations() != null
                && !user.getPreferredWorkLocations().isEmpty();
    }

    private boolean containsIgnoreCase(List<String> values, String expectedValue) {
        if (values == null || expectedValue == null) {
            return false;
        }

        return values.stream()
                .filter(Objects::nonNull)
                .anyMatch(value -> value.trim().equalsIgnoreCase(expectedValue.trim()));
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
                hiringPost.getClosureReason(),
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
