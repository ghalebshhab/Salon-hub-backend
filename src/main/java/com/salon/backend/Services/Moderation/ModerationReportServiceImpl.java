package com.salon.backend.Services.Moderation;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Community.CommunityAuthorResponse;
import com.salon.backend.DTOs.Moderation.*;
import com.salon.backend.Entities.community.*;
import com.salon.backend.Entities.moderation.*;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.*;
import com.salon.backend.Repositories.Community.*;
import com.salon.backend.Repositories.Moderation.ModerationReportRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModerationReportServiceImpl implements ModerationReportService {
    private final ModerationReportRepo reportRepo;
    private final CommunityPostRepo postRepo;
    private final CommunityCommentRepo commentRepo;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;

    @Override
    @Transactional
    public ApiResponse<ModerationReportResponse> createReport(CreateReportRequest request, Long reporterId) {
        User reporter = activeUser(reporterId);
        if (reporter == null) return ApiResponse.error("Active user not found");
        if (request == null || request.targetType() == null || request.targetId() == null || request.reason() == null)
            return ApiResponse.error("Report target and reason are required");
        String label = targetLabel(request.targetType(), request.targetId());
        if (label == null) return ApiResponse.error("The reported item is no longer available");
        if (reportRepo.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(reporterId, request.targetType(), request.targetId(), ReportStatus.OPEN))
            return ApiResponse.error("You already have an open report for this item");
        String details = clean(request.details());
        if (details != null && details.length() > 1000) return ApiResponse.error("Report details cannot exceed 1000 characters");
        ModerationReport report = new ModerationReport();
        report.setReporter(reporter);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setTargetLabel(label);
        report.setReason(request.reason());
        report.setDetails(details);
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(LocalDateTime.now());
        reportRepo.save(report);
        return ApiResponse.success("Report submitted for administrator review", map(report));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ModerationReportResponse>> getReports(ReportStatus status, Long adminId) {
        if (!isAdmin(adminId)) return ApiResponse.error("Administrator access required");
        List<ModerationReport> reports = status == null ? reportRepo.findAllByOrderByCreatedAtDesc() : reportRepo.findByStatusOrderByCreatedAtDesc(status);
        return ApiResponse.success("Moderation reports fetched successfully", reports.stream().map(this::map).toList());
    }

    @Override
    @Transactional
    public ApiResponse<ModerationReportResponse> reviewReport(Long reportId, ReviewReportRequest request, Long adminId) {
        User admin = userRepo.findById(adminId).filter(user -> user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.Active).orElse(null);
        if (admin == null) return ApiResponse.error("Administrator access required");
        if (request == null || (request.status() != ReportStatus.RESOLVED && request.status() != ReportStatus.DISMISSED))
            return ApiResponse.error("Report must be resolved or dismissed");
        ModerationReport report = reportId == null ? null : reportRepo.findById(reportId).orElse(null);
        if (report == null) return ApiResponse.error("Moderation report not found");
        String note = clean(request.adminNote());
        if (note != null && note.length() > 1000) return ApiResponse.error("Admin note cannot exceed 1000 characters");
        report.setStatus(request.status());
        report.setAdminNote(note);
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
        reportRepo.save(report);
        return ApiResponse.success("Moderation report updated", map(report));
    }

    private String targetLabel(ReportTargetType type, Long id) {
        return switch (type) {
            case POST -> postRepo.findById(id).map(post -> "Post by " + name(post.getAuthor()) + ": " + excerpt(post.getContent())).orElse(null);
            case COMMENT -> commentRepo.findById(id).map(comment -> "Comment by " + name(comment.getAuthor()) + ": " + excerpt(comment.getContent())).orElse(null);
            case SALON -> salonRepo.findById(id).map(Salon::getName).orElse(null);
            case USER -> userRepo.findById(id).filter(user -> user.getStatus() != UserStatus.Deleted).map(this::name).orElse(null);
        };
    }

    private String excerpt(String value) { return value == null ? "" : value.substring(0, Math.min(value.length(), 120)); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private User activeUser(Long id) { return id == null ? null : userRepo.findById(id).filter(user -> user.getStatus() == UserStatus.Active).orElse(null); }
    private boolean isAdmin(Long id) {
        User user = activeUser(id);
        return user != null && user.getRole() == UserRole.ADMIN;
    }
    private String name(User user) {
        String full = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return full.isBlank() ? user.getUserName() : full;
    }
    private CommunityAuthorResponse author(User user) { return user == null ? null : new CommunityAuthorResponse(user.getId(), user.getUserName(), name(user), user.getProfilePictureUrl()); }
    private ModerationReportResponse map(ModerationReport report) {
        return new ModerationReportResponse(report.getId(), author(report.getReporter()), report.getTargetType(), report.getTargetId(),
                report.getTargetLabel(), report.getReason(), report.getDetails(), report.getStatus(), report.getAdminNote(),
                author(report.getReviewedBy()), report.getCreatedAt(), report.getReviewedAt());
    }
}
