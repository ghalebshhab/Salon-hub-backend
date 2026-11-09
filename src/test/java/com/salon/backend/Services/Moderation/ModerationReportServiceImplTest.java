package com.salon.backend.Services.Moderation;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Moderation.CreateReportRequest;
import com.salon.backend.DTOs.Moderation.ModerationReportResponse;
import com.salon.backend.DTOs.Moderation.ReviewReportRequest;
import com.salon.backend.Entities.community.CommunityPost;
import com.salon.backend.Entities.moderation.ModerationReport;
import com.salon.backend.Entities.moderation.ReportReason;
import com.salon.backend.Entities.moderation.ReportStatus;
import com.salon.backend.Entities.moderation.ReportTargetType;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Community.CommunityCommentRepo;
import com.salon.backend.Repositories.Community.CommunityPostRepo;
import com.salon.backend.Repositories.Moderation.ModerationReportRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationReportServiceImplTest {

    @Mock private ModerationReportRepo reportRepo;
    @Mock private CommunityPostRepo postRepo;
    @Mock private CommunityCommentRepo commentRepo;
    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @InjectMocks private ModerationReportServiceImpl service;

    @Test
    void createsOpenPostReportWithTrimmedDetailsAndTargetSnapshot() {
        User reporter = user(1L, UserRole.USER, "Maya", "Stone");
        User author = user(2L, UserRole.USER, "Lina", "Rose");
        CommunityPost post = new CommunityPost();
        post.setId(20L);
        post.setAuthor(author);
        post.setContent("A post that should be checked");

        when(userRepo.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(postRepo.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(), ReportTargetType.POST, post.getId(), ReportStatus.OPEN)).thenReturn(false);

        ApiResponse<ModerationReportResponse> response = service.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), ReportReason.SPAM, "  Repeated promotion  "),
                reporter.getId());

        assertTrue(response.isSuccess());
        assertEquals(ReportStatus.OPEN, response.getData().status());
        assertEquals("Repeated promotion", response.getData().details());
        assertTrue(response.getData().targetLabel().startsWith("Post by Lina Rose"));

        ArgumentCaptor<ModerationReport> saved = ArgumentCaptor.forClass(ModerationReport.class);
        verify(reportRepo).save(saved.capture());
        assertEquals(reporter, saved.getValue().getReporter());
        assertNotNull(saved.getValue().getCreatedAt());
    }

    @Test
    void rejectsDuplicateOpenReportForTheSameTarget() {
        User reporter = user(1L, UserRole.USER, "Maya", "Stone");
        CommunityPost post = new CommunityPost();
        post.setId(20L);
        post.setAuthor(user(2L, UserRole.USER, "Lina", "Rose"));
        post.setContent("Post");

        when(userRepo.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(postRepo.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(), ReportTargetType.POST, post.getId(), ReportStatus.OPEN)).thenReturn(true);

        ApiResponse<ModerationReportResponse> response = service.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), ReportReason.OTHER, null),
                reporter.getId());

        assertFalse(response.isSuccess());
        verify(reportRepo, never()).save(any());
    }

    @Test
    void administratorCanResolveAReport() {
        User reporter = user(1L, UserRole.USER, "Maya", "Stone");
        User admin = user(99L, UserRole.ADMIN, "Admin", "User");
        ModerationReport report = new ModerationReport();
        report.setId(7L);
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(2L);
        report.setTargetLabel("Reported User");
        report.setReason(ReportReason.IMPERSONATION);
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(LocalDateTime.now());

        when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(reportRepo.findById(report.getId())).thenReturn(Optional.of(report));

        ApiResponse<ModerationReportResponse> response = service.reviewReport(
                report.getId(), new ReviewReportRequest(ReportStatus.RESOLVED, "  Reviewed and handled  "), admin.getId());

        assertTrue(response.isSuccess());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals("Reviewed and handled", report.getAdminNote());
        assertEquals(admin, report.getReviewedBy());
        assertNotNull(report.getReviewedAt());
        verify(reportRepo).save(report);
    }

    private User user(Long id, UserRole role, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(UserStatus.Active);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName((firstName + lastName).toLowerCase());
        return user;
    }
}
