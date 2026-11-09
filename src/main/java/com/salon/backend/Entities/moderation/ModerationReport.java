package com.salon.backend.Entities.moderation;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_report", indexes = {
        @Index(name = "idx_moderation_report_status_created", columnList = "status,created_at"),
        @Index(name = "idx_moderation_report_target", columnList = "target_type,target_id")
})
@Getter @Setter @NoArgsConstructor
public class ModerationReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private ReportTargetType targetType;
    @Column(nullable = false)
    private Long targetId;
    @Column(nullable = false)
    private String targetLabel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private ReportReason reason;
    @Column(length = 1000)
    private String details;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private ReportStatus status;
    @Column(length = 1000)
    private String adminNote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
