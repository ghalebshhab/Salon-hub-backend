package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.Services.Subscription.PlanLimits;
import com.salon.backend.Services.Email.WorkforceEmailNotifier;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostPageResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostSearchRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ReopenHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.hiringposts.ApplicationMatchLevel;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.salons.hiringposts.HiringPostClosureReason;
import com.salon.backend.Entities.salons.hiringposts.HiringApplication;
import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.HiringApplicationRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.LeaveRequestRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.Employment.EmploymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HiringPostServiceImplTest {

    @Mock
    private HiringPostRepo hiringPostRepo;
    @Mock
    private HiringApplicationRepo hiringApplicationRepo;
    @Mock
    private EmploymentRepo employmentRepo;
    @Mock
    private SalonRepo salonRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private EmploymentService employmentService;
    @Mock
    private LeaveRequestRepo leaveRequestRepo;
    @Mock
    private WorkforceEmailNotifier workforceEmails;
    @Mock
    private PlanLimits planLimits;

    @InjectMocks
    private HiringPostServiceImpl hiringPostService;

    @Test
    void ranksSuggestedPostsBeforePostsWithMissingRequirements() {
        User owner = new User();
        owner.setId(1L);
        owner.setFirstName("Salon");
        owner.setLastName("Owner");

        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Style Salon");
        salon.setLocation("Amman");
        salon.setStatus(SalonStatus.Accepted);
        salon.setOwner(owner);

        User applicant = new User();
        applicant.setCareerProfileEnabled(true);
        applicant.setDateOfBirth(LocalDate.of(1995, 1, 1));
        applicant.setYearsOfExperience(5);
        applicant.setCareerSkills(List.of("Haircut", "Coloring"));
        applicant.setPreferredWorkLocations(List.of("Amman", "Zarqa"));
        applicant.setResumeUrl("https://example.com/resume.pdf");

        HiringPost missingPost = createPost(
                20L,
                "Nail Specialist",
                List.of("Nail Art"),
                "Irbid",
                6,
                salon
        );
        HiringPost suggestedPost = createPost(
                21L,
                "Hair Stylist",
                List.of("Haircut"),
                "Amman",
                3,
                salon
        );

        when(userRepo.findById(applicant.getId())).thenReturn(Optional.of(applicant));
        when(hiringPostRepo.findAll(
                any(Specification.class),
                any(Sort.class)
        )).thenReturn(List.of(missingPost, suggestedPost));

        ApiResponse<HiringPostPageResponse> response =
                hiringPostService.searchOpenHiringPosts(
                        new HiringPostSearchRequest(),
                        applicant.getId()
                );

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getPersonalized()).isTrue();
        assertThat(response.getData().getContent()).hasSize(2);
        assertThat(response.getData().getContent().get(0).hiringPost().getId())
                .isEqualTo(21L);
        assertThat(response.getData().getContent().get(0).matchLevel())
                .isEqualTo(ApplicationMatchLevel.SUGGESTED);
        assertThat(response.getData().getContent().get(1).matchLevel())
                .isEqualTo(ApplicationMatchLevel.MISSING_REQUIREMENTS);
    }

    @Test
    void reopensPostWithRestoredAvailabilityWithoutAddingPositions() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setFirstName("Salon");
        owner.setLastName("Owner");

        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Style Salon");
        salon.setLocation("Amman");
        salon.setOwner(owner);

        HiringPost post = createPost(
                22L,
                "Hair Stylist",
                List.of("Haircut"),
                "Amman",
                3,
                salon
        );
        post.setStatus(HiringPostStatus.Closed);
        post.setClosureReason(HiringPostClosureReason.POSITIONS_FILLED);
        post.setNumOfPositions(1);

        when(hiringPostRepo.findByIdForUpdate(22L)).thenReturn(Optional.of(post));

        ApiResponse<?> response = hiringPostService.reopenHiringPost(
                22L,
                new ReopenHiringPostRequest(LocalDateTime.now().plusDays(20), 0),
                owner.getId()
        );

        assertThat(response.isSuccess()).isTrue();
        assertThat(post.getStatus()).isEqualTo(HiringPostStatus.Open);
        assertThat(post.getNumOfPositions()).isEqualTo(1);
        assertThat(post.getClosureReason()).isNull();
        verify(hiringPostRepo).save(post);
    }

    @Test
    void cancellingPendingTransferRetainsApplicationAndRestoresPosition() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setFirstName("Salon");
        owner.setLastName("Owner");

        Salon salon = new Salon();
        salon.setId(10L);
        salon.setName("Style Salon");
        salon.setLocation("Amman");
        salon.setOwner(owner);

        HiringPost post = createPost(
                23L,
                "Hair Stylist",
                List.of("Haircut"),
                "Amman",
                3,
                salon
        );
        post.setStatus(HiringPostStatus.Closed);
        post.setClosureReason(HiringPostClosureReason.POSITIONS_FILLED);
        post.setNumOfPositions(0);

        User applicant = new User();
        applicant.setId(2L);
        applicant.setEmail("applicant@example.com");

        EmploymentRequest employmentRequest = new EmploymentRequest();
        employmentRequest.setId(30L);
        employmentRequest.setStatus(EmploymentRequestStatus.Accepted_Pending_Leave);

        HiringApplication application = new HiringApplication();
        application.setId(40L);
        application.setApplicant(applicant);
        application.setHiringPost(post);
        application.setEmploymentRequest(employmentRequest);
        application.setDateOfBirth(LocalDate.of(1995, 1, 1));
        application.setYearsOfExperience(5);
        application.setSkills(List.of("Haircut", "Coloring"));
        application.setPreferredWorkLocations(List.of("Amman"));
        application.setCreatedAt(LocalDateTime.now());

        when(hiringApplicationRepo.findByIdForUpdate(40L))
                .thenReturn(Optional.of(application));
        when(hiringPostRepo.findByIdForUpdate(23L)).thenReturn(Optional.of(post));
        when(leaveRequestRepo.findByEmploymentRequest(employmentRequest))
                .thenReturn(Optional.empty());

        ApiResponse<HiringApplicationResponse> response =
                hiringPostService.cancelLinkedJoinRequest(
                        40L,
                        applicant.getId()
                );

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().salonLocation()).isEqualTo("Amman");
        assertThat(employmentRequest.getStatus())
                .isEqualTo(EmploymentRequestStatus.Cancelled);
        assertThat(post.getNumOfPositions()).isEqualTo(1);
        assertThat(post.getStatus()).isEqualTo(HiringPostStatus.Open);
        verify(hiringPostRepo).save(post);
        verify(employmentRepo).save(employmentRequest);
    }

    private HiringPost createPost(
            Long id,
            String title,
            List<String> requiredSkills,
            String location,
            int requiredExperience,
            Salon salon) {

        HiringPost post = new HiringPost();
        post.setId(id);
        post.setTitle(title);
        post.setDescription("A detailed hiring-post description");
        post.setRequiredSkills(requiredSkills);
        post.setMinimumAge(18);
        post.setMaximumAge(60);
        post.setEmployeeLocation(location);
        post.setMinimumYearsOfExperience(requiredExperience);
        post.setNumOfPositions(1);
        post.setStatus(HiringPostStatus.Open);
        post.setCreatedAt(LocalDateTime.now());
        post.setExpiresAt(LocalDateTime.now().plusDays(10));
        post.setSalon(salon);
        return post;
    }
}
